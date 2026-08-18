package vn.nguongocso.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import vn.nguongocso.report.dto.response.SystemStatusResponse;
import vn.nguongocso.report.enums.MetricStatus;
import vn.nguongocso.report.enums.OverallSystemStatus;
import vn.nguongocso.report.service.impl.SystemMonitoringServiceImpl;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class SystemMonitoringServiceImplTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private MetricsBufferService metricsBufferService;

    private SystemMonitoringServiceImpl systemMonitoringService;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        systemMonitoringService = new SystemMonitoringServiceImpl(dataSource, metricsBufferService);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(true);
    }

    @Test
    @DisplayName("TC-01: Luồng thành công - Tất cả các chỉ số ở mức bình thường (HEALTHY)")
    void getSystemStatus_HealthyWhenAllMetricsNormal() {
        when(metricsBufferService.hasSufficientData()).thenReturn(true);
        when(metricsBufferService.getServerErrorCountLastHour()).thenReturn(5L);
        when(metricsBufferService.getPublicTraceAvgLatencyLastHour()).thenReturn(300.0);
        when(metricsBufferService.getDataGatewayCallCountLastHour()).thenReturn(100L);

        SystemStatusResponse response = systemMonitoringService.getSystemStatus();

        assertNotNull(response);
        assertEquals(OverallSystemStatus.HEALTHY, response.getOverallStatus());
        assertTrue(response.isHasSufficientData());
        assertEquals(0, response.getBreachedMetricsCount());
        assertEquals(MetricStatus.NORMAL, response.getMetrics().get("serverErrorCount").getStatus());
    }

    @Test
    @DisplayName("TC-02: Ngoại lệ - Số lỗi máy chủ vượt 20 lần chuyển sang WARNING")
    void getSystemStatus_WarningWhenServerErrorExceedsThreshold() {
        when(metricsBufferService.hasSufficientData()).thenReturn(true);
        when(metricsBufferService.getServerErrorCountLastHour()).thenReturn(25L); // 25 > 20
        when(metricsBufferService.getPublicTraceAvgLatencyLastHour()).thenReturn(200.0);
        when(metricsBufferService.getDataGatewayCallCountLastHour()).thenReturn(100L);

        SystemStatusResponse response = systemMonitoringService.getSystemStatus();

        assertNotNull(response);
        assertEquals(OverallSystemStatus.WARNING, response.getOverallStatus());
        assertEquals(1, response.getBreachedMetricsCount());
        assertEquals(MetricStatus.WARNING, response.getMetrics().get("serverErrorCount").getStatus());
        assertTrue(response.getMetrics().get("serverErrorCount").getMessage().contains("25 lỗi"));
    }

    @Test
    @DisplayName("TC-04: Dữ liệu rỗng - Hệ thống vừa khởi động chưa đủ số liệu")
    void getSystemStatus_InsufficientDataWhenSystemJustRestarted() {
        when(metricsBufferService.hasSufficientData()).thenReturn(false);

        SystemStatusResponse response = systemMonitoringService.getSystemStatus();

        assertNotNull(response);
        assertEquals(OverallSystemStatus.INSUFFICIENT_DATA, response.getOverallStatus());
        assertFalse(response.isHasSufficientData());
        assertEquals(MetricStatus.INSUFFICIENT_DATA, response.getMetrics().get("serverErrorCount").getStatus());
    }
}
