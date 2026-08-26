package vn.nguongocso.trace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.dto.request.ExportLabelsRequest;
import vn.nguongocso.trace.dto.response.LabelExportResponse;
import vn.nguongocso.trace.entity.LabelExportHistory;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.LabelExportHistoryRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.impl.LabelExportServiceImpl;

/**
 * Unit test cho {@link LabelExportServiceImpl} (NCL-04-CN-005).
 */
@ExtendWith(MockitoExtension.class)
class LabelExportServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private LabelExportHistoryRepository labelExportHistoryRepository;

    @InjectMocks
    private LabelExportServiceImpl labelExportService;

    private CustomUserDetails currentUser;
    private Organization organization;
    private ProductionLot productionLot;
    private Shipment shipment;
    private List<TraceCode> traceCodes;
    private UUID organizationId;
    private UUID shipmentId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        shipmentId = UUID.randomUUID();

        organization = new Organization();
        organization.setOrganizationId(organizationId);
        organization.setName("HTX Nông Nghiệp Xanh");

        ProductCategory productCategory = new ProductCategory();
        productCategory.setName("Cà chua");

        productionLot = new ProductionLot();
        productionLot.setId(UUID.randomUUID());
        productionLot.setName("Lô cà chua vụ Đông");
        productionLot.setProductCategory(productCategory);
        productionLot.setOrganization(organization);

        shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("Lô hàng 01");
        shipment.setOrganization(organization);
        shipment.setProductionLot(productionLot);
        shipment.setStatus(ShipmentStatus.CODE_PRINTED);
        shipment.setCreatedAt(LocalDateTime.of(2026, 8, 25, 8, 0));

        traceCodes = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            TraceCode traceCode = new TraceCode();
            traceCode.setId(UUID.randomUUID());
            traceCode.setShipment(shipment);
            traceCode.setCodeValue(String.format("NGO-260825-%04d", i));
            traceCode.setStatus(TraceCodeStatus.INACTIVE);
            traceCodes.add(traceCode);
        }

        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setFullName("Nguyễn Văn A");

        currentUser = mock(CustomUserDetails.class);
        lenient().when(currentUser.getUserId()).thenReturn(user.getUserId());
        lenient().when(currentUser.getOrganizationId()).thenReturn(organizationId);
        lenient().when(currentUser.getRoleCode()).thenReturn("VT-02");
        lenient().when(currentUser.getUser()).thenReturn(user);

        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(currentUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ExportLabelsRequest.ExportLabelsRequestBuilder defaultRequest() {
        return ExportLabelsRequest.builder()
                .startIndex(0)
                .count(5)
                .labelSize("40x30");
    }

    private void stubHappyPath() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(traceCodeRepository.findByShipmentId(shipmentId)).thenReturn(traceCodes);
    }

    @Test
    @DisplayName("Xuất tem thành công: sinh PDF và ghi lịch sử xuất")
    void exportLabels_success() {
        stubHappyPath();

        LabelExportResponse response = labelExportService.exportLabels(
                shipmentId, defaultRequest().startIndex(2).count(5).build());

        assertThat(response.getPdfBytes()).isNotNull();
        // PDF magic header
        assertThat(new String(response.getPdfBytes(), 0, 4)).isEqualTo("%PDF");
        assertThat(response.getQuantity()).isEqualTo(5);
        assertThat(response.getStartIndex()).isEqualTo(2);
        assertThat(response.getEndIndex()).isEqualTo(6);
        assertThat(response.getLabelSize()).isEqualTo("40x30");

        ArgumentCaptor<LabelExportHistory> captor = ArgumentCaptor.forClass(LabelExportHistory.class);
        verify(labelExportHistoryRepository).save(captor.capture());
        LabelExportHistory saved = captor.getValue();
        assertThat(saved.getShipment()).isEqualTo(shipment);
        assertThat(saved.getOrganization()).isEqualTo(organization);
        assertThat(saved.getStartIndex()).isEqualTo(2);
        assertThat(saved.getEndIndex()).isEqualTo(6);
        assertThat(saved.getQuantity()).isEqualTo(5);
        assertThat(saved.getLabelSize()).isEqualTo("40x30");
    }

    @Test
    @DisplayName("404 khi lô hàng không tồn tại")
    void exportLabels_shipmentNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labelExportService.exportLabels(shipmentId, defaultRequest().build()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(labelExportHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("403 khi người dùng không phải VT-02")
    void exportLabels_forbiddenRole() {
        when(currentUser.getRoleCode()).thenReturn("VT-03");
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> labelExportService.exportLabels(shipmentId, defaultRequest().build()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(labelExportHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("403 khi lô hàng thuộc tổ chức khác (QTN-01)")
    void exportLabels_otherOrganization() {
        shipment.getOrganization().setOrganizationId(UUID.randomUUID());
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> labelExportService.exportLabels(shipmentId, defaultRequest().build()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("400 khi lô hàng đã bị thu hồi")
    void exportLabels_recalledShipment() {
        shipment.setStatus(ShipmentStatus.RECALLED);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> labelExportService.exportLabels(shipmentId, defaultRequest().build()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("400 khi khổ tem không hợp lệ")
    void exportLabels_invalidLabelSize() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> labelExportService.exportLabels(
                shipmentId, defaultRequest().labelSize("99x99").build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Khổ tem không hợp lệ");
    }

    @Test
    @DisplayName("400 khi lô hàng chưa có mã truy xuất")
    void exportLabels_noTraceCodes() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(traceCodeRepository.findByShipmentId(shipmentId)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> labelExportService.exportLabels(shipmentId, defaultRequest().build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa có mã truy xuất");
    }

    @Test
    @DisplayName("400 khi số lượng xuất vượt số mã đã sinh (QTN-23)")
    void exportLabels_countExceedsAvailable() {
        stubHappyPath();

        assertThatThrownBy(() -> labelExportService.exportLabels(
                shipmentId, defaultRequest().startIndex(0).count(11).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vượt quá số mã đã sinh");

        assertThatThrownBy(() -> labelExportService.exportLabels(
                shipmentId, defaultRequest().startIndex(8).count(3).build()))
                .isInstanceOf(BusinessException.class);

        verify(labelExportHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Xuất toàn bộ mã với startIndex mặc định 0")
    void exportLabels_exportAll() {
        stubHappyPath();

        LabelExportResponse response = labelExportService.exportLabels(
                shipmentId, ExportLabelsRequest.builder().count(10).labelSize("70x50").build());

        assertThat(response.getQuantity()).isEqualTo(10);
        assertThat(response.getStartIndex()).isZero();
        assertThat(response.getEndIndex()).isEqualTo(9);
        assertThat(new String(response.getPdfBytes(), 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("Trường thông tin rất dài vẫn sinh PDF hợp lệ ở mọi khổ tem (co chữ/ngắt dòng)")
    void exportLabels_longFieldValues_stillGeneratesPdf() {
        organization.setName(
                "Hop tac xa nong nghiep tong hop thi xa Son Tay va vung lan can dai nhat co the");
        productionLot.getProductCategory()
                .setName("Rau cu qua tuoi cac loai duoc trong theo tieu chuan VietGAP");
        productionLot.setName("Lo san xuat vu dong xuan nam 2026 tai vung nguyen lieu Tay Bac");
        stubHappyPath();

        for (String labelSize : new String[] {"40x30", "50x40", "70x50"}) {
            LabelExportResponse response = labelExportService.exportLabels(
                    shipmentId, defaultRequest().count(3).labelSize(labelSize).build());
            assertThat(response.getPdfBytes()).isNotNull();
            assertThat(new String(response.getPdfBytes(), 0, 4)).isEqualTo("%PDF");
        }
    }
}
