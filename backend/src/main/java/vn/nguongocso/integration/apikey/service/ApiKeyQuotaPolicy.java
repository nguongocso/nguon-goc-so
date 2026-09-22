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

    public double getQuotaWarningRatio() {
        return quotaWarningRatio;
    }
    public void setQuotaWarningRatio(double quotaWarningRatio) {
        this.quotaWarningRatio = quotaWarningRatio;
    }
    public int warningThreshold(int rateLimitPerHour) {
        if (rateLimitPerHour <= 0 || quotaWarningRatio <= 0) {
            return 0;
        }
        return (int) Math.ceil(rateLimitPerHour * quotaWarningRatio);
    }

    public boolean isReached(int usedCallsInDay, int rateLimitPerHour) {
        int threshold = warningThreshold(rateLimitPerHour);
        return threshold > 0 && usedCallsInDay >= threshold;
    }

    public int warningThresholdPercent() {
        return (int) Math.round(quotaWarningRatio * 100);
    }
}
