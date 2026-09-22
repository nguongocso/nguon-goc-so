package vn.nguongocso.certification.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.nguongocso.certification.service.CertificationService;

/**
 * Lớp CertificationExpiryScheduler chịu trách nhiệm quét và kiểm tra thời hạn chứng nhận.
 * Nó được cấu hình để chạy theo lịch trình định kỳ, mặc định là lúc 01:00 AM hàng ngày.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CertificationExpiryScheduler {
    private final CertificationService certificationService;

    /**
     * Tự động quét kiểm tra thời hạn chứng nhận.
     * Mặc định chạy lúc 01:00 AM hàng ngày.
     */
    @Scheduled(cron = "${app.certification.expiry-check-cron:0 0 1 * * ?}")
    public void scheduleExpiryCheck() {
        log.info("⏰ Bắt đầu chạy Scheduled Job: Quét kiểm tra thời hạn chứng nhận.");
        try {
            certificationService.checkCertificationExpiry();
            log.info("⏰ Hoàn thành chạy Scheduled Job: Quét kiểm tra thời hạn chứng nhận.");
        } catch (Exception e) {
            log.error("❌ Lỗi xảy ra trong quá trình chạy Scheduled Job quét hạn chứng nhận", e);
        }
    }
}
