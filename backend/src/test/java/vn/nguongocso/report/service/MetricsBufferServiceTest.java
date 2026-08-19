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

    @Test
    @DisplayName("Bucket cũ hơn 60 phút tự hết hạn và không được tính vào tổng")
    void staleBuckets_AreExcludedFromLastHour() throws Exception {
        metricsBufferService.recordServerError();
        metricsBufferService.recordServerError();
        assertEquals(2, metricsBufferService.getServerErrorCountLastHour());

        long currentMinute = System.currentTimeMillis() / 60000;
        java.util.concurrent.atomic.AtomicLongArray bucketTimestamps = getField("bucketTimestamps");
        bucketTimestamps.set((int) (currentMinute % 60), currentMinute - 61);

        assertEquals(0, metricsBufferService.getServerErrorCountLastHour());
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(String name) throws Exception {
        java.lang.reflect.Field field = MetricsBufferService.class.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(metricsBufferService);
    }
}
