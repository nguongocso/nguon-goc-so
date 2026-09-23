package vn.nguongocso.integration.apikey.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import vn.nguongocso.integration.apikey.service.ApiKeyWarningService;

/**
 * Scheduler đối soát cảnh báo hạn mức khóa truy cập.
*/
@Component
@RequiredArgsConstructor
public class ApiKeyQuotaWarningScheduler {
    private static final Logger log = LoggerFactory.getLogger(ApiKeyQuotaWarningScheduler.class);

    private final ApiKeyWarningService apiKeyWarningService;

    /**
     * Quét usage trong ngày và gửi bù cảnh báo hạn mức chưa được gửi.
     */
    @Scheduled(cron = "${app.apikey.quota-scan-cron:0 30 * * * ?}")
    public void scheduleQuotaReconcile() {
        log.info("Bắt đầu chạy Scheduled Job: Đối soát cảnh báo hạn mức khóa truy cập.");
        try {
            apiKeyWarningService.reconcileQuotaWarnings();
            log.info("Hoàn thành chạy Scheduled Job: Đối soát cảnh báo hạn mức khóa truy cập.");
        } catch (Exception e) {
            log.error("Lỗi xảy ra trong quá trình đối soát cảnh báo hạn mức khóa truy cập", e);
        }
    }
}
