package vn.nguongocso.alert.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.enums.ActivityLogExportStatus;
import vn.nguongocso.alert.repository.ActivityLogExportJobRepository;

/** Phân phối bền vững các yêu cầu xuất nền và khôi phục job bị gián đoạn. */
@Component
@Slf4j
public class ActivityLogExportDispatcher {
    private final ActivityLogExportJobRepository jobRepository;
    private final ActivityLogExportWorker worker;
    @Qualifier("applicationTaskExecutor")
    private final TaskExecutor taskExecutor;
    private final Clock clock;

    @Value("${app.activity-log-export.lease-seconds:300}")
    private long leaseSeconds;

    @Value("${app.activity-log-export.recovery-batch-size:20}")
    private int recoveryBatchSize;

    /**
     * Constructor.
     */
    public ActivityLogExportDispatcher(
            ActivityLogExportJobRepository jobRepository,
            ActivityLogExportWorker worker,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
            Clock clock) {
        this.jobRepository = jobRepository;
        this.worker = worker;
        this.taskExecutor = taskExecutor;
        this.clock = clock;
    }

    /** Thử giành quyền xử lý một job rồi chuyển cho executor trong bộ nhớ. */
    public boolean dispatch(UUID jobId) {
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(clock);
        int claimed = jobRepository.claim(jobId, token, now.plusSeconds(leaseSeconds), now,
                ActivityLogExportStatus.IN_PROGRESS);
        if (claimed == 0)
            return false;

        try {
            taskExecutor.execute(() -> worker.process(jobId, token));
            return true;
        } catch (RuntimeException exception) {
            jobRepository.releaseClaim(jobId, token);
            log.warn("Executor chưa thể tiếp nhận export job {}; job sẽ được thử lại.", jobId, exception);
            return false;
        }
    }

    /** Khôi phục ngay các job chưa được nhận hoặc đã hết lease sau khi ứng dụng sẵn sàng. */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        recoverPendingJobs();
    }

    /** Quét định kỳ để tự phục hồi khi tiến trình xử lý bị dừng đột ngột. */
    @Scheduled(fixedDelayString = "${app.activity-log-export.recovery-interval-ms:30000}", initialDelayString = "${app.activity-log-export.recovery-interval-ms:30000}")
    public void recoverPendingJobs() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<UUID> jobIds = jobRepository.findRecoverableJobIds(ActivityLogExportStatus.IN_PROGRESS, now,
                PageRequest.of(0, recoveryBatchSize));
        jobIds.forEach(this::dispatch);
    }
}
