package vn.nguongocso.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsBufferServiceTest {

    private MetricsBufferService metricsBufferService;

    @BeforeEach
    void setUp() {
        metricsBufferService = new MetricsBufferService();
        metricsBufferService.resetForTest();
    }

    @Test
    @DisplayName("Ghi nhận số lỗi server 5xx chính xác trong 1 giờ")
    void recordServerError_IncrementsErrorCount() {
        metricsBufferService.recordServerError();
        metricsBufferService.recordServerError();
        metricsBufferService.recordServerError();

        assertEquals(3, metricsBufferService.getServerErrorCountLastHour());
    }

    @Test
    @DisplayName("Tính thời gian phản hồi trung bình (latency) cho tra cứu công khai")
    void recordPublicTraceLatency_CalculatesAverageCorrectly() {
        metricsBufferService.recordPublicTraceLatency(100);
        metricsBufferService.recordPublicTraceLatency(200);
        metricsBufferService.recordPublicTraceLatency(300);

        assertEquals(200.0, metricsBufferService.getPublicTraceAvgLatencyLastHour(), 0.001);
    }

    @Test
    @DisplayName("Ghi nhận số lượt gọi Cổng dữ liệu")
    void recordDataGatewayCall_IncrementsCallCount() {
        metricsBufferService.recordDataGatewayCall();
        metricsBufferService.recordDataGatewayCall();

        assertEquals(2, metricsBufferService.getDataGatewayCallCountLastHour());
    }
}
