package vn.nguongocso.certification.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.nguongocso.certification.service.InspectionExpiryService;

/**
 * Lớp InspectionExpiryScheduler chịu trách nhiệm quét định kỳ thời hạn kết quả kiểm nghiệm
 * của các lô sản xuất và kích hoạt tạo cảnh báo/thông báo.
 * (NCL-11-CN-004)
 * Mặc định chạy vào lúc 00:00 (nửa đêm) hàng ngày.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InspectionExpiryScheduler {

    private final InspectionExpiryService inspectionExpiryService;

    /**
     * Tự động quét kiểm tra thời hạn kết quả kiểm nghiệm lô sản xuất.
     * Cấu hình qua app.inspection.expiry-check-cron, mặc định: 0 0 0 * * ? (00:00 AM hàng ngày).
     */
    @Scheduled(cron = "${app.inspection.expiry-check-cron:0 0 0 * * ?}")
    public void scheduleInspectionExpiryCheck() {
        log.info("⏰ Bắt đầu chạy Scheduled Job: Quét kiểm tra thời hạn kết quả kiểm nghiệm.");
        try {
            inspectionExpiryService.scanAndAlertExpiringInspections();
            log.info("⏰ Hoàn thành Scheduled Job: Quét kiểm tra thời hạn kết quả kiểm nghiệm.");
        } catch (Exception e) {
            log.error("❌ Lỗi xảy ra trong quá trình chạy Scheduled Job quét hạn kiểm nghiệm", e);
        }
    }
}
