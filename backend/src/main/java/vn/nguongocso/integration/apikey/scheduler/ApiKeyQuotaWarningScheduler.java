package vn.nguongocso.integration.apikey.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import vn.nguongocso.integration.apikey.service.ApiKeyWarningService;

/**
 * Scheduler đối soát cảnh báo hạn mức khóa truy cập (NCL-12-CN-005).
 * <p>
 * Chạy mỗi giờ (mặc định phút 30) để gửi bù cảnh báo "sắp chạm hạn mức" cho các
 * khóa đã vượt ngưỡng nhưng chưa được cảnh báo, ví dụ backend vừa khởi động lại,
 * chạy nhiều instance, hoặc lượt gọi vượt ngưỡng mà không trúng mốc bắn của luồng
 * realtime. Logic chi tiết nằm ở {@link ApiKeyWarningService#reconcileQuotaWarnings()}.
 * <p>
 * Chọn nhịp mỗi giờ thay vì quét hằng ngày lúc 00:00 vì mốc 00:00 là lúc sang
 * ngày mới (usage của ngày hôm đó bằng 0) nên quét đúng 00:00 sẽ không phát hiện được gì.
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
