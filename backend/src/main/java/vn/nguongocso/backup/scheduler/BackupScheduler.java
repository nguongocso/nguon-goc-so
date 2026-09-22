package vn.nguongocso.backup.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import vn.nguongocso.backup.entity.BackupSchedule;
import vn.nguongocso.backup.enums.BackupType;
import vn.nguongocso.backup.event.BackupScheduleChangedEvent;
import vn.nguongocso.backup.repository.BackupScheduleRepository;
import vn.nguongocso.backup.service.BackupService;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import org.springframework.beans.factory.annotation.Value;

/**
 * Lớp BackupScheduler chịu trách nhiệm quản lý lịch trình sao lưu dựa trên cấu hình trong cơ sở dữ liệu.
 * Nó lắng nghe sự kiện thay đổi lịch trình và cập nhật lịch trình sao lưu một cách động.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BackupScheduler {
    private final TaskScheduler taskScheduler;
    private final BackupScheduleRepository backupScheduleRepository;
    private final BackupService backupService;

    private ScheduledFuture<?> scheduledTask;

    @Value("${app.backup.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    /**
     * Khởi tạo scheduler ngay sau khi bean được tạo.
     */
    @PostConstruct
    public void init() {
        if (!schedulerEnabled) {
            log.info("Backup Scheduler is disabled.");
            return;
        }

        log.info("Initializing Backup Scheduler...");
        scheduleNext();
    }

    /**
     * Lập lịch công việc sao lưu tiếp theo. Phương thức này được đồng bộ để ngăn chặn xung đột.
     */
    public synchronized void scheduleNext() {
        if (scheduledTask != null) {
            log.info("Canceling existing backup schedule task...");
            scheduledTask.cancel(false);
            scheduledTask = null;
        }

        Optional<BackupSchedule> activeScheduleOpt = backupScheduleRepository.findFirstByIsActiveTrue();
        if (activeScheduleOpt.isPresent() && activeScheduleOpt.get().isActive()) {
            BackupSchedule schedule = activeScheduleOpt.get();
            String cron = schedule.getCronExpression();
            log.info("Scheduling backup job with cron: '{}' (Description: {})", cron, schedule.getDescription());

            try {
                scheduledTask = taskScheduler.schedule(
                        () -> {
                            log.info("Scheduled backup task triggered...");
                            try {
                                backupService.executeBackup(BackupType.SCHEDULED, null);
                            } catch (Exception e) {
                                log.error("Error executing scheduled backup", e);
                            }
                        },
                        new CronTrigger(cron));
            } catch (Exception e) {
                log.error("Failed to schedule task with cron expression '{}'", cron, e);
            }
        } else {
            log.info("No active backup schedule configuration found in database.");
        }
    }

    /**
     * Lắng nghe sự kiện BackupScheduleChangedEvent và tải lại cấu hình động.
     */
    @EventListener
    public void handleScheduleChanged(BackupScheduleChangedEvent event) {
        log.info("Backup schedule changed event received. Reloading schedule dynamic configuration...");
        scheduleNext();
    }
}
