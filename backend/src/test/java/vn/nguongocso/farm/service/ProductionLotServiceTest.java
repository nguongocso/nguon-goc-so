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
