package vn.nguongocso.alert.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    /** Xử lý một job đã được commit và snapshot hoàn tất. */
    public void process(UUID jobId) {
        ActivityLogExportJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != ActivityLogExportStatus.IN_PROGRESS) return;

        Path output = null;
        try {
            Path directory = Paths.get(storageDirectory).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String fileName = "activity-logs-" + jobId + "-"
                    + LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            output = directory.resolve(fileName).normalize();
            if (!output.startsWith(directory)) throw new IOException("Đường dẫn tệp export không hợp lệ.");

            try (CSVPrinter printer = csvWriter.openFile(output)) {
                int pageNumber = 0;
                Page<ActivityLogExportItem> page;
                do {
                    page = itemRepository.findByJobId(jobId,
                            PageRequest.of(pageNumber++, PAGE_SIZE, Sort.by("sequenceNo").ascending()));
                    for (ActivityLogExportItem item : page.getContent()) csvWriter.print(printer, item);
                } while (page.hasNext());
            }

            job.setStatus(ActivityLogExportStatus.SUCCESS);
            job.setFileName(fileName);
            job.setFilePath(output.toString());
            job.setFileSize(Files.size(output));
            job.setCompletedAt(LocalDateTime.now(clock));
            jobRepository.save(job);
            try {
                notificationService.sendActivityLogExportReadyNotification(jobId, job.getRequestedBy());
            } catch (RuntimeException notificationError) {
                log.error("Không thể gửi thông báo export nhật ký đã hoàn tất cho job {}", jobId, notificationError);
            }
        } catch (Exception error) {
            if (output != null) {
                try { Files.deleteIfExists(output); } catch (IOException cleanupError) {
                    log.warn("Không thể xóa tệp export lỗi {}", output, cleanupError);
                }
            }
            job.setStatus(ActivityLogExportStatus.FAILED);
            job.setErrorMessage("Không thể tạo tệp nhật ký hoạt động.");
            job.setCompletedAt(LocalDateTime.now(clock));
            jobRepository.save(job);
            log.error("Xử lý export nhật ký nền thất bại cho job {}", jobId, error);
        }
    }

    /** Đánh dấu job thất bại khi executor không thể tiếp nhận tác vụ sau commit. */
    public void markDispatchFailed(UUID jobId) {
        ActivityLogExportJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != ActivityLogExportStatus.IN_PROGRESS) return;
        job.setStatus(ActivityLogExportStatus.FAILED);
        job.setErrorMessage("Không thể khởi tạo xử lý tệp nhật ký nền.");
        job.setCompletedAt(LocalDateTime.now(clock));
        jobRepository.save(job);
    }
}
