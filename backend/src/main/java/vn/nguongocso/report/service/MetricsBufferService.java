package vn.nguongocso.report.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Service lưu trữ và quản lý bộ đệm số liệu theo cửa sổ trượt 60 phút (Sliding Window).
 * Đảm bảo thread-safe, không sử dụng lock gây nghẽn hiệu năng.
 */
@Service
public class MetricsBufferService {

    private static final int BUCKET_COUNT = 60; // 60 phút trong 1 giờ
    private final long startTimeMs = System.currentTimeMillis();

    private final AtomicLongArray bucketTimestamps = new AtomicLongArray(BUCKET_COUNT);
    private final AtomicLongArray serverErrors = new AtomicLongArray(BUCKET_COUNT);
    private final AtomicLongArray publicTraceLatencySum = new AtomicLongArray(BUCKET_COUNT);
    private final AtomicLongArray publicTraceCalls = new AtomicLongArray(BUCKET_COUNT);
    private final AtomicLongArray dataGatewayCalls = new AtomicLongArray(BUCKET_COUNT);

    private final AtomicLong totalRecordedRequests = new AtomicLong(0);

    /**
     * Ghi nhận 1 lỗi máy chủ HTTP 5xx.
     */
    public void recordServerError() {
        int index = getBucketIndexAndResetIfStale();
        serverErrors.incrementAndGet(index);
        totalRecordedRequests.incrementAndGet();
    }

    /**
     * Ghi nhận thời gian xử lý (latency) của một lượt tra cứu công khai.
     * @param durationMs Thời gian phản hồi tính bằng ms
     */
    public void recordPublicTraceLatency(long durationMs) {
        int index = getBucketIndexAndResetIfStale();
        publicTraceLatencySum.addAndGet(index, Math.max(0, durationMs));
        publicTraceCalls.incrementAndGet(index);
        totalRecordedRequests.incrementAndGet();
    }

    /**
     * Ghi nhận 1 lượt gọi vào Cổng dữ liệu / API tích hợp.
     */
    public void recordDataGatewayCall() {
        int index = getBucketIndexAndResetIfStale();
        dataGatewayCalls.incrementAndGet(index);
        totalRecordedRequests.incrementAndGet();
    }

    /**
     * Lấy tổng số lỗi máy chủ 5xx trong 60 phút gần nhất.
     */
    public long getServerErrorCountLastHour() {
        long nowMinute = System.currentTimeMillis() / 60000;
        long totalErrors = 0;
        for (int i = 0; i < BUCKET_COUNT; i++) {
            long bucketTime = bucketTimestamps.get(i);
            if (nowMinute - bucketTime < BUCKET_COUNT) {
                totalErrors += serverErrors.get(i);
            }
        }
        return totalErrors;
    }

    /**
     * Lấy thời gian phản hồi trung bình (ms) của tra cứu công khai trong 60 phút gần nhất.
     * Trả về -1.0 nếu chưa có lượt tra cứu nào.
     */
    public double getPublicTraceAvgLatencyLastHour() {
        long nowMinute = System.currentTimeMillis() / 60000;
        long totalLatency = 0;
        long totalCalls = 0;
        for (int i = 0; i < BUCKET_COUNT; i++) {
            long bucketTime = bucketTimestamps.get(i);
            if (nowMinute - bucketTime < BUCKET_COUNT) {
                totalLatency += publicTraceLatencySum.get(i);
                totalCalls += publicTraceCalls.get(i);
            }
        }
        return totalCalls > 0 ? (double) totalLatency / totalCalls : -1.0;
    }

    /**
     * Lấy tổng số lượt gọi Cổng dữ liệu trong 60 phút gần nhất.
     */
    public long getDataGatewayCallCountLastHour() {
        long nowMinute = System.currentTimeMillis() / 60000;
        long totalCalls = 0;
        for (int i = 0; i < BUCKET_COUNT; i++) {
            long bucketTime = bucketTimestamps.get(i);
            if (nowMinute - bucketTime < BUCKET_COUNT) {
                totalCalls += dataGatewayCalls.get(i);
            }
        }
        return totalCalls;
    }

    /**
     * Kiểm tra hệ thống đã có đủ số liệu giám sát hay chưa (phục vụ kịch bản TC-04).
     */
    public boolean hasSufficientData() {
        long uptimeSeconds = getUptimeSeconds();
        if (uptimeSeconds < 60 && totalRecordedRequests.get() == 0) {
            return false;
        }
        return true;
    }

    public long getUptimeSeconds() {
        return Math.max(0, (System.currentTimeMillis() - startTimeMs) / 1000);
    }

    /**
     * Đặt lại bộ đệm cho các mục đích kiểm thử (Testing helper).
     */
    public void resetForTest() {
        for (int i = 0; i < BUCKET_COUNT; i++) {
            bucketTimestamps.set(i, 0);
            serverErrors.set(i, 0);
            publicTraceLatencySum.set(i, 0);
            publicTraceCalls.set(i, 0);
            dataGatewayCalls.set(i, 0);
        }
        totalRecordedRequests.set(0);
    }

    private int getBucketIndexAndResetIfStale() {
        long currentMinute = System.currentTimeMillis() / 60000;
        int index = (int) (currentMinute % BUCKET_COUNT);
        long oldMinute = bucketTimestamps.get(index);
        if (oldMinute != currentMinute) {
            if (bucketTimestamps.compareAndSet(index, oldMinute, currentMinute)) {
                serverErrors.set(index, 0);
                publicTraceLatencySum.set(index, 0);
                publicTraceCalls.set(index, 0);
                dataGatewayCalls.set(index, 0);
            }
        }
        return index;
    }
}
