package vn.nguongocso.export.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.export.dto.request.ExportOpenDataRequest;
import vn.nguongocso.export.dto.response.OpenDataExportJobResponse;
import vn.nguongocso.export.entity.OpenDataExportJob;
import vn.nguongocso.export.enums.ExportJobStatus;
import vn.nguongocso.export.repository.OpenDataExportJobRepository;
import vn.nguongocso.export.service.OpenDataAsyncExportService;
import vn.nguongocso.export.service.worker.OpenDataExportWorker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Triển khai dịch vụ xuất dữ liệu mở bất đồng bộ với cơ chế Polling và tự động dọn dẹp file tạm theo TTL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenDataAsyncExportServiceImpl implements OpenDataAsyncExportService {

    private final OpenDataExportJobRepository jobRepository;
    private final OpenDataExportWorker exportWorker;

    @Value("${app.export.temp-dir:#{systemProperties['java.io.tmpdir'] + '/nguongocso-exports'}}")
    private String tempDirPath;

    @Override
    @Transactional
    public OpenDataExportJobResponse submitJob(ExportOpenDataRequest request, CustomUserDetails currentUser) {
        String format = (request.getFormat() != null ? request.getFormat() : "JSON").toUpperCase();
        OpenDataExportJob job = OpenDataExportJob.builder()
                .id(UUID.randomUUID())
                .requestedBy(currentUser != null ? currentUser.getUserId() : null)
                .requestedByUsername(currentUser != null ? currentUser.getUsername() : "system")
                .status(ExportJobStatus.PENDING)
                .format(format)
                .createdAt(LocalDateTime.now())
                .build();

        OpenDataExportJob savedJob = jobRepository.save(job);
        log.info("Đã tạo Export Job: id={}, user={}, format={}", savedJob.getId(), savedJob.getRequestedByUsername(), format);

        // Ủy thác cho worker xử lý ngầm trong thread pool
        exportWorker.processExportJob(savedJob.getId(), request, currentUser, tempDirPath);

        return toResponse(savedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public OpenDataExportJobResponse getJobStatus(UUID jobId) {
        OpenDataExportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tác vụ xuất dữ liệu với ID: " + jobId));
        return toResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getJobDownload(UUID jobId) {
        OpenDataExportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tác vụ xuất dữ liệu với ID: " + jobId));

        if (job.getStatus() == ExportJobStatus.IN_PROGRESS || job.getStatus() == ExportJobStatus.PENDING) {
            throw new BusinessException("Tác vụ đang trong quá trình xử lý, vui lòng kiểm tra lại sau.");
        }
        if (job.getStatus() == ExportJobStatus.FAILED) {
            throw new BusinessException("Tác vụ xuất dữ liệu đã thất bại: " + job.getErrorMessage());
        }

        if (job.getFilePath() == null) {
            throw new ResourceNotFoundException("Đường dẫn tệp kết xuất không tồn tại.");
        }

        File file = new File(job.getFilePath());
        if (!file.exists() || !file.isFile()) {
            throw new ResourceNotFoundException("Tệp kết xuất không còn tồn tại trên hệ thống (có thể đã hết hạn lưu trữ).");
        }

        return new FileSystemResource(file);
    }

    @Override
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredJobs() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<OpenDataExportJob> expiredJobs = jobRepository.findByCreatedAtBefore(threshold);
        for (OpenDataExportJob job : expiredJobs) {
            if (job.getFilePath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(job.getFilePath()));
                } catch (IOException e) {
                    log.warn("Không thể xóa tệp tạm hết hạn: {}", job.getFilePath(), e);
                }
            }
        }
        int deleted = jobRepository.deleteByCreatedAtBefore(threshold);
        if (deleted > 0) {
            log.info("Đã dọn dẹp {} tác vụ xuất dữ liệu cũ hơn 24 giờ", deleted);
        }
    }

    private OpenDataExportJobResponse toResponse(OpenDataExportJob job) {
        return OpenDataExportJobResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .format(job.getFormat())
                .fileName(job.getFileName())
                .fileSize(job.getFileSize())
                .downloadUrl("/api/v1/export/jobs/" + job.getId() + "/download")
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .errorMessage(job.getErrorMessage())
                .build();
    }
}
