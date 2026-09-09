package vn.nguongocso.farm.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.nguongocso.farm.dto.response.MilestoneScanResult;
import vn.nguongocso.farm.service.MilestoneReminderService;

/**
 * Lớp lập lịch quét mốc canh tác bắt buộc quá hạn (NCL-03-CN-007).
 *
 * <p>Mặc định chạy lúc 02:00 AM hằng ngày.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MilestoneReminderScheduler {

    private final MilestoneReminderService milestoneReminderService;

    /**
     * Tự động quét và tạo nhắc việc cho mốc canh tác quá hạn lúc 02:00 AM hằng ngày.
     */
    @Scheduled(cron = "${app.milestone.reminder-cron:0 0 2 * * ?}")
    public void scheduleOverdueMilestoneScan() {
        log.info("⏰ Bắt đầu Scheduled Job: Quét mốc canh tác quá hạn.");
        try {
            MilestoneScanResult result = milestoneReminderService.scanOverdueMilestones();
            log.info("⏰ Hoàn thành Scheduled Job: {}", result.getMessage());
        } catch (Exception e) {
            log.error("❌ Lỗi xảy ra trong quá trình chạy Scheduled Job quét mốc canh tác quá hạn", e);
        }
    }
}
