package vn.nguongocso.report.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.report.dto.response.CropAreaAnalysisResponse;
import vn.nguongocso.report.service.impl.CropAreaAnalysisServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
/**
 * Test service CropAreaAnalysisService.
 *
 * @author Triệu Văn Đại
 */
@ExtendWith(MockitoExtension.class)
public class CropAreaAnalysisServiceTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private ReportAccessLogService reportAccessLogService;

    @InjectMocks
    private CropAreaAnalysisServiceImpl cropAreaAnalysisService;

    private CustomUserDetails regulatorDetails;
    private CustomUserDetails adminDetails;
    private CustomUserDetails managerDetails;

    private UUID userId;
    private UUID userOrgId;
    private UUID targetOrgId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userOrgId = UUID.randomUUID();
        targetOrgId = UUID.randomUUID();

        // Mock CustomUserDetails cho Cán bộ quản lý ngành (VT-05)
        regulatorDetails = mock(CustomUserDetails.class);
        lenient().when(regulatorDetails.getUserId()).thenReturn(userId);
        lenient().when(regulatorDetails.getOrganizationId()).thenReturn(userOrgId);
        lenient().when(regulatorDetails.getRoleCode()).thenReturn("VT-05");

        // Mock CustomUserDetails cho Admin (VT-01)
        adminDetails = mock(CustomUserDetails.class);
        lenient().when(adminDetails.getUserId()).thenReturn(userId);
        lenient().when(adminDetails.getOrganizationId()).thenReturn(userOrgId);
        lenient().when(adminDetails.getRoleCode()).thenReturn("VT-01");

        // Mock CustomUserDetails cho Quản lý HTX (VT-02)
        managerDetails = mock(CustomUserDetails.class);
        lenient().when(managerDetails.getUserId()).thenReturn(userId);
        lenient().when(managerDetails.getOrganizationId()).thenReturn(userOrgId);
        lenient().when(managerDetails.getRoleCode()).thenReturn("VT-02");
    }

    @Test
    void getAnalysis_shouldSucceed_whenRegulatorQueriesWithData() {
        // Given
        Integer year = 2026;
        String ipAddress = "127.0.0.1";

        Organization organization = Organization.builder()
                .organizationId(targetOrgId)
                .name("HTX Chè Tân Cương")
                .build();

        ProductCategory cropType = ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Chè Tân Cương")
                .build();

        FarmArea farmArea = FarmArea.builder()
                .id(UUID.randomUUID())
                .name("Vùng Trồng Chè A")
                .area(new BigDecimal("5.5"))
                .organization(organization)
                .build();

        // Lô 1: Vụ Đông Xuân (Tháng 3)
        ProductionLot lot1 = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô Chè Đông Xuân 1")
                .farmArea(farmArea)
                .productCategory(cropType)
                .organization(organization)
                .expectedQuantity(10000.0)
                .actualQuantity(9500.0)
                .plantingDate(LocalDate.of(2026, 3, 15))
                .build();

        // Lô 2: Vụ Hè Thu (Tháng 7)
        ProductionLot lot2 = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô Chè Hè Thu 2")
                .farmArea(farmArea)
                .productCategory(cropType)
                .organization(organization)
                .expectedQuantity(12000.0)
                .actualQuantity(11000.0)
                .plantingDate(LocalDate.of(2026, 7, 10))
                .build();

        List<ProductionLot> rawLots = new ArrayList<>();
        rawLots.add(lot1);
        rawLots.add(lot2);

        LocalDate startDate = LocalDate.of(2025, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);

        when(productionLotRepository.findLotsForAnalysis(startDate, endDate, null, null, null))
                .thenReturn(rawLots);

        // When
        CropAreaAnalysisResponse response = cropAreaAnalysisService.getAnalysis(
                year, null, null, null, regulatorDetails, ipAddress);

        // Then
        // 1. Kiểm tra ghi log thành công
        verify(reportAccessLogService).logAccess(userId, userOrgId, userOrgId, "CROP_AREA_ANALYSIS", true, ipAddress);

        // 2. Kiểm tra Summary Stats
        assertThat(response.getSummary().getTotalLots()).isEqualTo(2L);
        assertThat(response.getSummary().getTotalExpectedYield()).isEqualTo(22000.0);
        assertThat(response.getSummary().getTotalActualYield()).isEqualTo(20500.0);
        assertThat(response.getSummary().getTotalArea()).isEqualTo(5.5); // Không lặp diện tích

        // 3. Kiểm tra Thống kê theo vùng (byArea)
        assertThat(response.getByArea()).hasSize(1);
        assertThat(response.getByArea().get(0).getFarmAreaName()).isEqualTo("Vùng Trồng Chè A");
        assertThat(response.getByArea().get(0).getSeasons()).hasSize(2); // Có Đông Xuân & Hè Thu

        // 4. Kiểm tra Thống kê theo mùa vụ (bySeason)
        assertThat(response.getBySeason()).hasSize(2);

        // Tìm và assert mùa vụ Đông Xuân
        CropAreaAnalysisResponse.SeasonAnalysisStats dongXuan = response.getBySeason().stream()
                .filter(s -> "DONG_XUAN".equals(s.getSeasonCode()))
                .findFirst().orElseThrow();
        assertThat(dongXuan.getTotalLots()).isEqualTo(1L);
        assertThat(dongXuan.getExpectedYield()).isEqualTo(10000.0);
        assertThat(dongXuan.getActualYield()).isEqualTo(9500.0);
    }

    @Test
    void getAnalysis_shouldThrowAccessDenied_whenUserIsOrgManager() {
        // Given
        String ipAddress = "192.168.1.100";

        // When & Then
        assertThatThrownBy(() -> cropAreaAnalysisService.getAnalysis(
                2026, null, null, null, managerDetails, ipAddress))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Bạn không có quyền truy cập báo cáo phân tích ngành.");

        // Xác nhận đã ghi log truy cập thất bại để audit
        verify(reportAccessLogService).logAccess(userId, userOrgId, userOrgId, "CROP_AREA_ANALYSIS", false, ipAddress);
        verifyNoInteractions(productionLotRepository);
    }

    @Test
    void getAnalysis_shouldReturnEmptyResponse_whenNoDataFound() {
        // Given
        LocalDate startDate = LocalDate.of(2025, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);
        String ipAddress = "127.0.0.1";

        when(productionLotRepository.findLotsForAnalysis(startDate, endDate, null, null, null))
                .thenReturn(Collections.emptyList());

        // When
        CropAreaAnalysisResponse response = cropAreaAnalysisService.getAnalysis(
                2026, null, null, null, adminDetails, ipAddress);

        // Then
        verify(reportAccessLogService).logAccess(userId, userOrgId, userOrgId, "CROP_AREA_ANALYSIS", true, ipAddress);
        assertThat(response.getSummary().getTotalLots()).isEqualTo(0L);
        assertThat(response.getSummary().getTotalArea()).isEqualTo(0.0);
        assertThat(response.getByArea()).isEmpty();
        assertThat(response.getBySeason()).isEmpty();
    }
}
