package vn.nguongocso.alert.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.entity.ActivityLogExportItem;
import vn.nguongocso.alert.entity.ActivityLogExportJob;
import vn.nguongocso.alert.enums.ActivityLogExportStatus;
import vn.nguongocso.alert.repository.ActivityLogExportItemRepository;
import vn.nguongocso.alert.repository.ActivityLogExportJobRepository;
import vn.nguongocso.notification.service.NotificationService;

/** Worker sinh tệp CSV cho yêu cầu export nền. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityLogExportWorker {
    private static final int PAGE_SIZE = 1_000;

    private final ActivityLogExportJobRepository jobRepository;
    private final ActivityLogExportItemRepository itemRepository;
    private final ActivityLogCsvWriter csvWriter;
    private final NotificationService notificationService;
    private final Clock clock;

    @Value("${app.activity-log-export.storage-dir:./uploads/activity-log-exports}")
    private String storageDirectory;

    @Value("${app.activity-log-export.lease-seconds:300}")
    private long leaseSeconds;

    /** Xử lý một job đã giành được lease và có snapshot hoàn tất. */
    public void process(UUID jobId, String processingToken) {
        ActivityLogExportJob job = jobRepository
                .findByIdAndProcessingTokenAndStatus(jobId, processingToken, ActivityLogExportStatus.IN_PROGRESS)
                .orElse(null);
        if (job == null) return;

        Path output = null;
        try {
            Path directory = Paths.get(storageDirectory).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String fileName = "activity-logs-" + jobId + "-"
                    + LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            output = directory.resolve(fileName).normalize();
            if (!output.startsWith(directory)) throw new IOException("Đường dẫn tệp export không hợp lệ.");

            try (CSVPrinter printer = csvWriter.openFile(output)) {
                long lastSequenceNo = -1L;
                List<ActivityLogExportItem> items;
                do {
                    if (!renewLease(jobId, processingToken)) {
                        throw new LeaseLostException();
                    }
                    items = itemRepository.findByJobIdAndSequenceNoGreaterThanOrderBySequenceNoAsc(
                            jobId, lastSequenceNo, PageRequest.of(0, PAGE_SIZE));
                    for (ActivityLogExportItem item : items) csvWriter.print(printer, item);
                    if (!items.isEmpty()) lastSequenceNo = items.get(items.size() - 1).getSequenceNo();
                } while (items.size() == PAGE_SIZE);
            }

            if (!renewLease(jobId, processingToken)) {
                deleteOutput(output);
                return;
            }
            job.setStatus(ActivityLogExportStatus.SUCCESS);
            job.setFileName(fileName);
            job.setFilePath(output.toString());
            job.setFileSize(Files.size(output));
            job.setCompletedAt(LocalDateTime.now(clock));
            job.setProcessingToken(null);
            job.setLeaseExpiresAt(null);
            jobRepository.save(job);
            try {
                notificationService.sendActivityLogExportReadyNotification(jobId, job.getRequestedBy());
            } catch (RuntimeException notificationError) {
                log.error("Không thể gửi thông báo export nhật ký đã hoàn tất cho job {}", jobId, notificationError);
            }
        } catch (LeaseLostException exception) {
            deleteOutput(output);
            log.warn("Dừng export job {} vì worker không còn giữ lease.", jobId);
        } catch (Exception error) {
            deleteOutput(output);
            if (renewLeaseSafely(jobId, processingToken)) {
                job.setStatus(ActivityLogExportStatus.FAILED);
                job.setErrorMessage("Không thể tạo tệp nhật ký hoạt động.");
                job.setCompletedAt(LocalDateTime.now(clock));
                job.setProcessingToken(null);
                job.setLeaseExpiresAt(null);
                jobRepository.save(job);
            }
            log.error("Xử lý export nhật ký nền thất bại cho job {}", jobId, error);
        }
    }

    private boolean renewLease(UUID jobId, String processingToken) {
        return jobRepository.renewLease(jobId, processingToken,
                LocalDateTime.now(clock).plusSeconds(leaseSeconds), ActivityLogExportStatus.IN_PROGRESS) == 1;
    }

    private boolean renewLeaseSafely(UUID jobId, String processingToken) {
        try {
            return renewLease(jobId, processingToken);
        } catch (RuntimeException exception) {
            log.warn("Không thể gia hạn lease cho export job {}.", jobId, exception);
            return false;
        }
    }

    private void deleteOutput(Path output) {
        if (output == null) return;
        try {
            Files.deleteIfExists(output);
        } catch (IOException cleanupError) {
            log.warn("Không thể xóa tệp export lỗi {}", output, cleanupError);
        }
    }

    private static final class LeaseLostException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
