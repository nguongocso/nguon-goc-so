package vn.nguongocso.integration.apikey.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Quy tắc ngưỡng cảnh báo hạn mức của khóa truy cập đối tác (NCL-12-CN-005).
 * <p>
 * Là nơi duy nhất tính ngưỡng cảnh báo trong backend để đường gọi API của đối
 * tác, job đối soát và trang cảnh báo tổng hợp luôn nhất quán:
 * {@code ngưỡng = ceil(rateLimitPerHour × tỷ lệ cảnh báo)}.
 */
@Component
public class ApiKeyQuotaPolicy {

    @Value("${app.apikey.quota-warning-ratio:0.8}")
    private double quotaWarningRatio;

    /**
     * Tỷ lệ hạn mức chạm ngưỡng cảnh báo (0.8 = 80%).
     */
    public double getQuotaWarningRatio() {
        return quotaWarningRatio;
    }

    /**
     * Số lượt gọi trong ngày chạm ngưỡng cảnh báo của một khóa.
     *
     * @param rateLimitPerHour hạn mức lượt gọi mỗi giờ của khóa
     * @return ngưỡng cảnh báo; trả 0 nếu hạn mức hoặc tỷ lệ cấu hình không hợp lệ
     */
    public int warningThreshold(int rateLimitPerHour) {
        if (rateLimitPerHour <= 0 || quotaWarningRatio <= 0) {
            return 0;
        }
        return (int) Math.ceil(rateLimitPerHour * quotaWarningRatio);
    }

    /**
     * Kiểm tra số lượt gọi trong ngày đã chạm ngưỡng cảnh báo hay chưa.
     */
    public boolean isReached(int usedCallsInDay, int rateLimitPerHour) {
        int threshold = warningThreshold(rateLimitPerHour);
        return threshold > 0 && usedCallsInDay >= threshold;
    }

    /**
     * Tỷ lệ ngưỡng cảnh báo dạng phần trăm để hiển thị trong nội dung cảnh báo.
     */
    public int warningThresholdPercent() {
        return (int) Math.round(quotaWarningRatio * 100);
    }
}
