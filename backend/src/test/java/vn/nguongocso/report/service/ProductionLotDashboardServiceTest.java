// 9. UNIT TEST: ProductionLotDashboardTest.java
// Package: vn.nguongocso.farm.service

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
import vn.nguongocso.report.dto.response.ProductionLotDashboardResponse;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.impl.ProductionLotServiceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
/**
 * Test service ProductionLotDashboard.
 *
 * @author Triệu Văn Đại
 */
@ExtendWith(MockitoExtension.class)
public class ProductionLotDashboardServiceTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private ReportAccessLogService reportAccessLogService;

    @InjectMocks
    private ProductionLotServiceImpl productionLotService;

    private CustomUserDetails managerDetails;
    private CustomUserDetails adminDetails;
    private UUID userOrgId;
    private UUID otherOrgId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userOrgId = UUID.randomUUID();
        otherOrgId = UUID.randomUUID();
        userId = UUID.randomUUID();

        managerDetails = mock(CustomUserDetails.class);

        adminDetails = mock(CustomUserDetails.class);
        lenient().when(adminDetails.getOrganizationId()).thenReturn(userOrgId);
        lenient().when(adminDetails.getUserId()).thenReturn(userId);
        lenient().when(adminDetails.getRoleCode()).thenReturn("VT-01");
    }

    @Test
    void getDashboard_shouldSucceed_whenManagerQueriesOwnOrg() {

        when(managerDetails.getOrganizationId()).thenReturn(userOrgId);
        when(managerDetails.getUserId()).thenReturn(userId);
        when(managerDetails.getRoleCode()).thenReturn("VT-02");
        // Given
        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 31);
        String ipAddress = "127.0.0.1";

        List<Object[]> summaryData = new ArrayList<>();
        summaryData.add(new Object[]{ProductionLotStatus.APPROVED, 3L, 300.0, 250.0});
        summaryData.add(new Object[]{ProductionLotStatus.HARVESTED, 2L, 200.0, 180.0});

        List<Object[]> timeSeriesData = new ArrayList<>();
        timeSeriesData.add(new Object[]{LocalDate.of(2026, 6, 15), 100.0, 90.0});
        timeSeriesData.add(new Object[]{LocalDate.of(2026, 6, 20), 200.0, 160.0});
        timeSeriesData.add(new Object[]{LocalDate.of(2026, 7, 10), 200.0, 180.0});

        when(productionLotRepository.getDashboardSummaryAndStatus(userOrgId, startDate, endDate))
                .thenReturn(summaryData);
        when(productionLotRepository.getDashboardTimeSeriesData(userOrgId, startDate, endDate))
                .thenReturn(timeSeriesData);

        // When
        ProductionLotDashboardResponse response = productionLotService.getDashboard(
                startDate, endDate, userOrgId, "MONTH", managerDetails, ipAddress);

        // Then
        verify(reportAccessLogService).logAccess(userId, userOrgId, userOrgId, "YIELD_AND_LOT_DASHBOARD", true, ipAddress);

        // Verify Summary
        assertThat(response.getSummary().getTotalLots()).isEqualTo(5L);
        assertThat(response.getSummary().getTotalExpectedYield()).isEqualTo(500.0);
        assertThat(response.getSummary().getTotalActualYield()).isEqualTo(430.0);

        // Verify byStatus counts
        assertThat(response.getByStatus().get("APPROVED")).isEqualTo(3L);
        assertThat(response.getByStatus().get("HARVESTED")).isEqualTo(2L);
        assertThat(response.getByStatus().get("DRAFT")).isEqualTo(0L); // Default value

        // Verify TimeSeries grouping by MONTH
        assertThat(response.getTimeSeries()).hasSize(2);
        assertThat(response.getTimeSeries().get(0).getPeriod()).isEqualTo("2026-06");
        assertThat(response.getTimeSeries().get(0).getLotCount()).isEqualTo(2L);
        assertThat(response.getTimeSeries().get(0).getExpectedYield()).isEqualTo(300.0);

        assertThat(response.getTimeSeries().get(1).getPeriod()).isEqualTo("2026-07");
        assertThat(response.getTimeSeries().get(1).getLotCount()).isEqualTo(1L);
        assertThat(response.getTimeSeries().get(1).getExpectedYield()).isEqualTo(200.0);
    }

    @Test
    void getDashboard_shouldExcludeCancelledLots_fromSummaryTotals() {
        // NCL-02-CN-006: lô đã hủy chỉ thống kê riêng ở byStatus["CANCELLED"],
        // không tính vào tổng số lô/sản lượng đang canh tác (TC-08).
        when(managerDetails.getOrganizationId()).thenReturn(userOrgId);
        when(managerDetails.getUserId()).thenReturn(userId);
        when(managerDetails.getRoleCode()).thenReturn("VT-02");

        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 31);
        String ipAddress = "127.0.0.1";

        List<Object[]> summaryData = new ArrayList<>();
        summaryData.add(new Object[]{ProductionLotStatus.APPROVED, 2L, 200.0, 150.0});
        summaryData.add(new Object[]{ProductionLotStatus.CANCELLED, 1L, 100.0, 0.0});

        when(productionLotRepository.getDashboardSummaryAndStatus(userOrgId, startDate, endDate))
                .thenReturn(summaryData);
        when(productionLotRepository.getDashboardTimeSeriesData(userOrgId, startDate, endDate))
                .thenReturn(Collections.emptyList());

        // When
        ProductionLotDashboardResponse response = productionLotService.getDashboard(
                startDate, endDate, userOrgId, "MONTH", managerDetails, ipAddress);

        // Then
        assertThat(response.getByStatus().get("CANCELLED")).isEqualTo(1L);
        assertThat(response.getSummary().getTotalLots()).isEqualTo(2L);
        assertThat(response.getSummary().getTotalExpectedYield()).isEqualTo(200.0);
        assertThat(response.getSummary().getTotalActualYield()).isEqualTo(150.0);
        assertThat(response.getTimeSeries()).isEmpty();
    }


    @Test
    void getDashboard_shouldThrowAccessDenied_whenManagerQueriesOtherOrg() {
        when(managerDetails.getOrganizationId()).thenReturn(userOrgId);
        when(managerDetails.getUserId()).thenReturn(userId);
        when(managerDetails.getRoleCode()).thenReturn("VT-02");
        // Given
        String ipAddress = "192.168.1.50";

        // When & Then
        assertThatThrownBy(() -> productionLotService.getDashboard(
                null, null, otherOrgId, "MONTH", managerDetails, ipAddress))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Từ chối truy cập");

        // Verify failure access log was saved
        verify(reportAccessLogService).logAccess(userId, userOrgId, otherOrgId, "YIELD_AND_LOT_DASHBOARD", false, ipAddress);
        verifyNoInteractions(productionLotRepository);
    }

    @Test
    void getDashboard_shouldSucceed_whenAdminQueriesOtherOrg() {
        // Given
        String ipAddress = "127.0.0.1";
        when(productionLotRepository.getDashboardSummaryAndStatus(otherOrgId, null, null))
                .thenReturn(Collections.emptyList());
        when(productionLotRepository.getDashboardTimeSeriesData(otherOrgId, null, null))
                .thenReturn(Collections.emptyList());

        // When
        ProductionLotDashboardResponse response = productionLotService.getDashboard(
                null, null, otherOrgId, "MONTH", adminDetails, ipAddress);

        // Then
        verify(reportAccessLogService).logAccess(userId, userOrgId, otherOrgId, "YIELD_AND_LOT_DASHBOARD", true, ipAddress);
        assertThat(response.getSummary().getTotalLots()).isEqualTo(0L);
    }
}
