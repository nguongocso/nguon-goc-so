package vn.nguongocso.integration.apikey.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.integration.apikey.service.ApiKeyWarningService;

/**
 * Scheduler quét hằng ngày khóa truy cập sắp hết hạn (NCL-12-CN-005).
 * <p>
 * Mặc định chạy lúc 01:00 sáng. Logic chi tiết nằm ở {@link ApiKeyWarningService}.
 */
@Component
@RequiredArgsConstructor
public class ApiKeyWarningScheduler {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyWarningScheduler.class);

    private final ApiKeyWarningService apiKeyWarningService;

    /**
     * Quét khóa sắp hết hạn, persist khóa đã quá hạn và gửi cảnh báo.
     */
    @Scheduled(cron = "${app.apikey.expiry-scan-cron:0 0 1 * * ?}")
    public void scheduleExpiryScan() {
        log.info("Bắt đầu chạy Scheduled Job: Quét cảnh báo khóa truy cập sắp hết hạn.");
        try {
            apiKeyWarningService.scanExpiringKeys();
            log.info("Hoàn thành chạy Scheduled Job: Quét cảnh báo khóa truy cập sắp hết hạn.");
        } catch (Exception e) {
            log.error("Lỗi xảy ra trong quá trình quét cảnh báo khóa truy cập", e);
        }
    }
}
