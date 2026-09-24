package vn.nguongocso.export.service.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.export.dto.request.ExportOpenDataRequest;
import vn.nguongocso.export.entity.OpenDataExportJob;
import vn.nguongocso.export.enums.ExportJobStatus;
import vn.nguongocso.export.repository.OpenDataExportJobRepository;
import vn.nguongocso.export.service.ExportService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Worker xử lý ngầm tác vụ xuất dữ liệu mở trên luồng riêng biệt của thread pool.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenDataExportWorker {

    private final OpenDataExportJobRepository jobRepository;
    private final ExportService exportService;

    /**
     * Thực thi tác vụ xuất dữ liệu ngầm bất đồng bộ.
     */
    @Async("exportTaskExecutor")
    public void processExportJob(UUID jobId, ExportOpenDataRequest request, CustomUserDetails currentUser, String tempDirPath) {
        OpenDataExportJob job = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            job = jobRepository.findById(jobId).orElse(null);
            if (job != null) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (job == null) {
            log.error("Không tìm thấy OpenDataExportJob với ID: {} sau 5 lần kiểm tra", jobId);
            return;
        }

        job.setStatus(ExportJobStatus.IN_PROGRESS);
        jobRepository.save(job);

        try {
            Resource resource = exportService.exportOpenData(request, currentUser);
            byte[] fileBytes = resource.getInputStream().readAllBytes();

            Path exportDir = Paths.get(tempDirPath);
            if (!Files.exists(exportDir)) {
                Files.createDirectories(exportDir);
            }

            String format = (job.getFormat() != null ? job.getFormat() : "json").toLowerCase();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "open_data_" + timestamp + "_" + job.getId().toString().substring(0, 8) + "." + format;
            Path targetFile = exportDir.resolve(fileName);

            Files.write(targetFile, fileBytes);

            job.setStatus(ExportJobStatus.COMPLETED);
            job.setFileName(fileName);
            job.setFilePath(targetFile.toAbsolutePath().toString());
            job.setFileSize((long) fileBytes.length);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);

            log.info("Hoàn tất tác vụ xuất dữ liệu bất đồng bộ: jobId={}, fileName={}, size={}", jobId, fileName, fileBytes.length);
        } catch (Exception e) {
            log.error("Thất bại khi thực hiện tác vụ xuất dữ liệu bất đồng bộ: jobId={}", jobId, e);
            job.setStatus(ExportJobStatus.FAILED);
            job.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Lỗi xử lý tệp xuất dữ liệu");
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
        }
    }
}
