package vn.nguongocso.integration.apikey.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Quy tắc ngưỡng cảnh báo hạn mức của khóa truy cập đối tác.
*/
@Component
public class ApiKeyQuotaPolicy {
    @Value("${app.apikey.quota-warning-ratio:0.8}")
    private double quotaWarningRatio;

    /**
     * Lấy tỷ lệ hạn mức chạm ngưỡng cảnh báo.
     */
    public double getQuotaWarningRatio() {
        return quotaWarningRatio;
    }

    /**
     * Gán tỷ lệ hạn mức chạm ngưỡng cảnh báo.
     */
    public void setQuotaWarningRatio(double quotaWarningRatio) {
        this.quotaWarningRatio = quotaWarningRatio;
    }

    /**
     * Tính số lượt gọi chạm ngưỡng cảnh báo của một khóa.
     */
    public int warningThreshold(int rateLimitPerHour) {
        if (rateLimitPerHour <= 0 || quotaWarningRatio <= 0) {
            return 0;
        }
        return (int) Math.ceil(rateLimitPerHour * quotaWarningRatio);
    }

    /**
     * Kiểm tra số lượt gọi đã chạm ngưỡng cảnh báo hay chưa.
     */
    public boolean isReached(int usedCallsInDay, int rateLimitPerHour) {
        int threshold = warningThreshold(rateLimitPerHour);
        return threshold > 0 && usedCallsInDay >= threshold;
    }

    /**
     * Lấy tỷ lệ ngưỡng cảnh báo dạng phần trăm để hiển thị.
     */
    public int warningThresholdPercent() {
        return (int) Math.round(quotaWarningRatio * 100);
    }
}
