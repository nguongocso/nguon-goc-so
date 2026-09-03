package vn.nguongocso.farm.service;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.request.ApproveProductionLotRequest;
import vn.nguongocso.farm.dto.request.CancelProductionLotRequest;
import vn.nguongocso.farm.dto.request.CreateProductionLotRequest;
import vn.nguongocso.farm.dto.response.CreateProductionLotResponse;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.impl.ProductionLotServiceImpl;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ProductionLotServiceTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private FarmAreaRepository farmAreaRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductionLotServiceImpl productionLotService;

    private CustomUserDetails userDetails;
    private UUID orgId;
    private UUID userId;
    private UUID lotId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();
        lotId = UUID.randomUUID();
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getOrganizationId()).thenReturn(orgId);
        when(userDetails.getUserId()).thenReturn(userId);
    }

    @Test
    void approveProductionLot_shouldApprove_whenLotIsPendingAndApprovedTrue() {

        // Given
        ApproveProductionLotRequest request = new ApproveProductionLotRequest();

        request.setApproved(true);

        ProductionLot lot = createPendingLot();
        User approver = new User();
        approver.setUserId(userId);
        approver.setFullName("Approver");


        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(approver));
        when(productionLotRepository.save(any(ProductionLot.class))).thenReturn(lot);

        // When
        CreateProductionLotResponse response = productionLotService.approveProductionLot(lotId, request, userDetails);

        // Then
        assertThat(response.getStatus()).isEqualTo(ProductionLotStatus.APPROVED.name());
        verify(productionLotRepository).save(lot);
        assertThat(lot.getApprovedBy()).isEqualTo(approver);
        assertThat(lot.getApprovalNotes()).isNull();
    }

    private ProductionLot createPendingLot() {

        Organization org = new Organization();
        org.setOrganizationId(orgId);
        FarmArea farm = new FarmArea();
        ProductCategory category = new ProductCategory();
        ProductionLot lot = ProductionLot.builder()
                .id(lotId)
                .organization(org)
                .farmArea(farm)
                .productCategory(category)
                .name("Test lot")
                .expectedQuantity(100.0)
                .expectedQuantityUnit("kg")
                .status(ProductionLotStatus.PENDING)
                .build();

        return lot;
    }

    @Test
    void approveProductionLot_shouldRejectAndSetDraft_whenApprovedFalse() {
    // Given
    ApproveProductionLotRequest request = new ApproveProductionLotRequest();
    request.setApproved(false);
    request.setReason("Thiếu thông tin vùng trồng");

        ProductionLot lot = createPendingLot();
        User approver = new User();

        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(approver));
        when(productionLotRepository.save(any(ProductionLot.class))).thenReturn(lot);

        CreateProductionLotResponse response = productionLotService.approveProductionLot(lotId, request, userDetails);

        assertThat(response.getStatus()).isEqualTo(ProductionLotStatus.DRAFT.name());
        assertThat(lot.getApprovalNotes()).isEqualTo("Thiếu thông tin vùng trồng");
        assertThat(lot.getApprovedBy()).isNull();
    }

    @Test
    void approveProductionLot_shouldThrow_whenLotNotFound() {
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.empty());
        ApproveProductionLotRequest request = new ApproveProductionLotRequest();
        request.setApproved(true);

        assertThatThrownBy(() -> productionLotService.approveProductionLot(lotId, request, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không tìm thấy lô sản xuất");
    }

    @Test
    void approveProductionLot_shouldThrow_whenNotBelongToOrg() {
        ProductionLot lot = createPendingLot();
        Organization otherOrg = new Organization();
        otherOrg.setOrganizationId(UUID.randomUUID());
        lot.setOrganization(otherOrg);

        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));

        ApproveProductionLotRequest request = new ApproveProductionLotRequest();
        request.setApproved(true);

        assertThatThrownBy(() -> productionLotService.approveProductionLot(lotId, request, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lô sản xuất không thuộc tổ chức của bạn");
    }

    @Test
    void approveProductionLot_shouldThrow_whenLotStatusNotPending() {
        ProductionLot lot = createPendingLot();
        lot.setStatus(ProductionLotStatus.DRAFT);

        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));
        ApproveProductionLotRequest request = new ApproveProductionLotRequest();
        request.setApproved(true);

        assertThatThrownBy(() -> productionLotService.approveProductionLot(lotId, request, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Chỉ có thể duyệt lô đang ở trạng thái chờ duyệt");
    }

    // =========================================================
    // NCL-02-CN-006: Hủy lô sản xuất và ghi lý do
    // =========================================================

    @Test
    void cancelProductionLot_shouldCancelLot_whenCancellableAndNoTraceCodes() {
        // Given
        ProductionLot lot = createPendingLot();
        User canceller = new User();
        canceller.setUserId(userId);
        canceller.setFullName("Quản lý HTX");

        CancelProductionLotRequest request = new CancelProductionLotRequest();
        request.setReason("khai báo nhầm");
        request.setNote("Khai báo sai vùng trồng");

        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));
        when(shipmentRepository.findByProductionLotId(lotId)).thenReturn(List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.of(canceller));
        when(productionLotRepository.save(any(ProductionLot.class))).thenReturn(lot);

        // When
        CreateProductionLotResponse response =
                productionLotService.cancelProductionLot(lotId, request, userDetails);

        // Then
        assertThat(response.getStatus()).isEqualTo(ProductionLotStatus.CANCELLED.name());
        assertThat(lot.getStatus()).isEqualTo(ProductionLotStatus.CANCELLED);
        assertThat(lot.getCancellationReason()).isEqualTo("khai báo nhầm");
        assertThat(lot.getCancellationNote()).isEqualTo("Khai báo sai vùng trồng");
        assertThat(lot.getCancelledBy()).isEqualTo(canceller);
        assertThat(lot.getCancelledAt()).isNotNull();
        verify(productionLotRepository).save(lot);
    }

    @Test
    void cancelProductionLot_shouldThrow_whenNotBelongToOrg() {
        // Given — QTN-01: cách ly dữ liệu giữa các tổ chức
        ProductionLot lot = createPendingLot();
        Organization otherOrg = new Organization();
        otherOrg.setOrganizationId(UUID.randomUUID());
        lot.setOrganization(otherOrg);

        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));

        CancelProductionLotRequest request = new CancelProductionLotRequest();
        request.setReason("sâu bệnh");
        request.setNote("Sâu bệnh nặng toàn vùng");

        // When & Then
        assertThatThrownBy(() -> productionLotService.cancelProductionLot(lotId, request, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lô sản xuất không thuộc tổ chức của bạn");
        verify(productionLotRepository, never()).save(any(ProductionLot.class));
    }

    @Test
    void cancelProductionLot_shouldThrow_whenLotInTerminalState() {
        // Given
        ProductionLot lot = createPendingLot();
        lot.setStatus(ProductionLotStatus.CANCELLED);

        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));

        CancelProductionLotRequest request = new CancelProductionLotRequest();
        request.setReason("lý do khác");
        request.setNote("Lô đã hủy trước đó");

        // When & Then
        assertThatThrownBy(() -> productionLotService.cancelProductionLot(lotId, request, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lô đã ở trạng thái CANCELLED, không thể hủy");
        verify(productionLotRepository, never()).save(any(ProductionLot.class));
    }

    @Test
    void cancelProductionLot_shouldThrow_whenLotHasTraceCodes() {
        // Given — TC-02: lô đã sinh mã truy xuất phải dùng luồng thu hồi
        ProductionLot lot = createPendingLot();

        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));
        when(shipmentRepository.findByProductionLotId(lotId))
                .thenReturn(List.of(mock(Shipment.class)));

        CancelProductionLotRequest request = new CancelProductionLotRequest();
        request.setReason("khai báo nhầm");
        request.setNote("Lô đã đóng gói sinh mã rồi");

        // When & Then
        assertThatThrownBy(() -> productionLotService.cancelProductionLot(lotId, request, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lô đã sinh mã truy xuất, không thể hủy. Vui lòng sử dụng luồng thu hồi lô");
        verify(productionLotRepository, never()).save(any(ProductionLot.class));
    }

    private CreateProductionLotRequest createLotRequest(UUID farmAreaId) {
        CreateProductionLotRequest request = new CreateProductionLotRequest();
        request.setName("Lô xoài Cát Chu");
        request.setFarmAreaId(farmAreaId);
        request.setProductCategoryId(UUID.randomUUID());
        request.setExpectedQuantity(100.0);
        request.setExpectedQuantityUnit("kg");
        return request;
    }

    @Test
    void createProductionLot_shouldThrow_whenFarmAreaIdIsMissing() {
        // Given
        CreateProductionLotRequest request = createLotRequest(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(new Organization()));
        when(productCategoryRepository.findById(request.getProductCategoryId()))
                .thenReturn(Optional.of(new ProductCategory()));

        // When & Then
        assertThatThrownBy(() -> productionLotService.createProductionLot(request, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vui lòng chọn vùng trồng");
        verify(productionLotRepository, never()).save(any(ProductionLot.class));
    }

    @Test
    void createProductionLot_shouldThrow_whenFarmAreaBelongsToOtherOrg() {
        // Given
        UUID farmAreaId = UUID.randomUUID();
        CreateProductionLotRequest request = createLotRequest(farmAreaId);

        Organization otherOrg = new Organization();
        otherOrg.setOrganizationId(UUID.randomUUID());
        FarmArea otherOrgFarmArea = new FarmArea();
        otherOrgFarmArea.setId(farmAreaId);
        otherOrgFarmArea.setOrganization(otherOrg);

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(new Organization()));
        when(productCategoryRepository.findById(request.getProductCategoryId()))
                .thenReturn(Optional.of(new ProductCategory()));
        when(farmAreaRepository.findById(farmAreaId)).thenReturn(Optional.of(otherOrgFarmArea));

        // When & Then
        assertThatThrownBy(() -> productionLotService.createProductionLot(request, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Khu vực canh tác này không thuộc tổ chức của bạn");
        verify(productionLotRepository, never()).save(any(ProductionLot.class));
    }
}
