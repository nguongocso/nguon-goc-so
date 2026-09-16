package vn.nguongocso.alert.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import vn.nguongocso.alert.dto.request.ActivityLogExportFilterRequest;
import vn.nguongocso.alert.dto.response.ActivityLogExportDownload;
import vn.nguongocso.alert.dto.response.ActivityLogExportJobResponse;
import vn.nguongocso.alert.dto.response.ActivityLogExportPreviewResponse;
import vn.nguongocso.alert.dto.response.ActivityLogExportResult;
import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.entity.ActivityLogExportJob;
import vn.nguongocso.alert.enums.ActivityLogExportStatus;
import vn.nguongocso.alert.repository.ActivityLogExportItemRepository;
import vn.nguongocso.alert.repository.ActivityLogExportJobRepository;
import vn.nguongocso.alert.repository.ActivityLogRepository;
import vn.nguongocso.alert.service.ActivityLogExportService;
import vn.nguongocso.alert.specification.ActivityLogSpecification;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;

/** Triển khai xuất trực tiếp hoặc xử lý nền theo quy mô snapshot. */
@Service
public class ActivityLogExportServiceImpl implements ActivityLogExportService {
    public static final int DEFAULT_DIRECT_EXPORT_LIMIT = 10_000;
    private static final String ROLE_ORGANIZATION_MANAGER = "VT-02";
    private static final String DIRECT_MODE = "DIRECT";
    private static final String ASYNC_MODE = "ASYNC";

    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogExportJobRepository jobRepository;
    private final ActivityLogExportItemRepository itemRepository;
    private final ActivityLogCsvWriter csvWriter;
    private final ActivityLogExportDispatcher dispatcher;
    private final Clock clock;

    @Value("${app.activity-log-export.direct-limit:10000}")
    private int directExportLimit = DEFAULT_DIRECT_EXPORT_LIMIT;

    public static final int DEFAULT_MAX_RANGE_DAYS = 365;

    @Value("${app.activity-log-export.max-range-days:365}")
    private int maxRangeDays = DEFAULT_MAX_RANGE_DAYS;

    @Value("${app.activity-log-export.storage-dir:./uploads/activity-log-exports}")
    private String storageDirectory;

    public ActivityLogExportServiceImpl(
            ActivityLogRepository activityLogRepository,
            ActivityLogExportJobRepository jobRepository,
            ActivityLogExportItemRepository itemRepository,
            ActivityLogCsvWriter csvWriter,
            ActivityLogExportDispatcher dispatcher,
            Clock clock) {
        this.activityLogRepository = activityLogRepository;
        this.jobRepository = jobRepository;
        this.itemRepository = itemRepository;
        this.csvWriter = csvWriter;
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityLogExportPreviewResponse preview(ActivityLogExportFilterRequest request, CustomUserDetails user) {
        validateRequest(request, user);
        long count = activityLogRepository.count(buildSpecification(request, user));
        return ActivityLogExportPreviewResponse.builder()
                .count(count)
                .mode(count > directExportLimit ? ASYNC_MODE : DIRECT_MODE)
                .build();
    }

    @Override
    @Transactional
    public ActivityLogExportResult requestExport(ActivityLogExportFilterRequest request, CustomUserDetails user) {
        validateRequest(request, user);
        Specification<ActivityLog> specification = buildSpecification(request, user);
        long count = activityLogRepository.count(specification);
        if (count == 0) throw new BusinessException("Không có nhật ký hoạt động trong phạm vi lọc.");

        if (count <= directExportLimit) {
            Page<ActivityLog> page = activityLogRepository.findAll(specification,
                    PageRequest.of(0, directExportLimit + 1, snapshotSort()));
            if (page.getContent().size() <= directExportLimit) {
                byte[] csv = csvWriter.writeActivities(page.getContent());
                saveExportAudit(request, user, page.getContent().size(), "SUCCESS", null);
                return ActivityLogExportResult.builder().mode(DIRECT_MODE).csvBytes(csv).build();
            }
        }

        ActivityLogExportJob job = createAsyncJob(request, user);
        saveExportAudit(request, user, job.getRecordCount(), "IN_PROGRESS", job.getId());
        dispatchAfterCommit(job.getId());
        return ActivityLogExportResult.builder().mode(ASYNC_MODE).job(toResponse(job)).build();
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityLogExportJobResponse getJob(UUID exportId, CustomUserDetails user) {
        validateUser(user);
        return toResponse(findTenantJob(exportId, user));
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityLogExportDownload getDownload(UUID exportId, CustomUserDetails user) {
        validateUser(user);
        ActivityLogExportJob job = findTenantJob(exportId, user);
        if (job.getStatus() == ActivityLogExportStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT, "Tệp nhật ký đang được xử lý.");
        }
        if (job.getStatus() == ActivityLogExportStatus.FAILED) {
            throw new BusinessException(HttpStatus.CONFLICT, "Yêu cầu xuất nhật ký đã thất bại.");
        }
        Path base = Paths.get(storageDirectory).toAbsolutePath().normalize();
        Path path = Paths.get(job.getFilePath()).toAbsolutePath().normalize();
        if (!path.startsWith(base) || !Files.isRegularFile(path)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Tệp nhật ký không còn tồn tại.");
        }
        return ActivityLogExportDownload.builder()
                .path(path).fileName(job.getFileName()).fileSize(job.getFileSize()).build();
    }

    private ActivityLogExportJob createAsyncJob(ActivityLogExportFilterRequest request, CustomUserDetails user) {
        ActivityLogExportJob job = jobRepository.save(ActivityLogExportJob.builder()
                .organizationId(user.getOrganizationId()).requestedBy(user.getUserId())
                .requestedByUsername(user.getUsername()).requestedByRole(user.getRoleCode())
                .startDate(request.getStartDate()).endDate(request.getEndDate())
                .actionFilter(request.getAction()).actorFilter(request.getActorName())
                .objectTypeFilter(request.getObjectType()).status(ActivityLogExportStatus.IN_PROGRESS)
                .recordCount(0L).createdAt(LocalDateTime.now(clock)).build());

        int snapshotCount = itemRepository.snapshotFromActivityLogs(
                job.getId().toString(), user.getOrganizationId().toString(),
                request.getStartDate() == null ? null : request.getStartDate().atStartOfDay(),
                request.getEndDate() == null ? null : request.getEndDate().atTime(23, 59, 59, 999_999_999),
                normalized(request.getAction()), normalized(request.getActorName()), normalized(request.getObjectType()));
        job.setRecordCount((long) snapshotCount);
        return jobRepository.save(job);
    }

    private void dispatchAfterCommit(UUID jobId) {
        Runnable dispatch = () -> dispatcher.dispatch(jobId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { dispatch.run(); }
            });
        } else dispatch.run();
    }

    private ActivityLogExportJob findTenantJob(UUID jobId, CustomUserDetails user) {
        return jobRepository.findByIdAndOrganizationId(jobId, user.getOrganizationId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu xuất nhật ký."));
    }

    private ActivityLogExportJobResponse toResponse(ActivityLogExportJob job) {
        boolean success = job.getStatus() == ActivityLogExportStatus.SUCCESS;
        return ActivityLogExportJobResponse.builder().exportId(job.getId()).mode(ASYNC_MODE)
                .status(job.getStatus().name()).recordCount(job.getRecordCount())
                .fileName(job.getFileName()).fileSize(job.getFileSize()).createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .downloadUrl(success ? "/api/v1/organizations/activity-logs/exports/" + job.getId() + "/download" : null)
                .build();
    }

    private Specification<ActivityLog> buildSpecification(ActivityLogExportFilterRequest request, CustomUserDetails user) {
        return ActivityLogSpecification.hasOrganizationId(user.getOrganizationId())
                .and(ActivityLogSpecification.createdBetween(request.getStartDate(), request.getEndDate()))
                .and(ActivityLogSpecification.hasAction(request.getAction()))
                .and(ActivityLogSpecification.hasActorName(request.getActorName()))
                .and(ActivityLogSpecification.hasEntityType(request.getObjectType()));
    }

    private Sort snapshotSort() {
        return Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
    }

    private void validateRequest(ActivityLogExportFilterRequest request, CustomUserDetails user) {
        validateUser(user);
        if (request == null) throw new BusinessException("Bộ lọc xuất nhật ký không được để trống.");
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new BusinessException("Ngày bắt đầu không được sau ngày kết thúc.");
            }
            long daysBetween = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
            if (daysBetween > maxRangeDays) {
                throw new BusinessException(
                        String.format("Khoảng thời gian xuất nhật ký không được vượt quá %d ngày.", maxRangeDays));
            }
        }
    }

    private void validateUser(CustomUserDetails user) {
        if (user == null || !ROLE_ORGANIZATION_MANAGER.equals(user.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Chỉ Quản lý tổ chức (VT-02) mới được xuất nhật ký hoạt động.");
        }
        if (user.getOrganizationId() == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Người dùng không thuộc tổ chức nào.");
        }
    }

    private void saveExportAudit(ActivityLogExportFilterRequest request, CustomUserDetails user,
            long recordCount, String status, UUID jobId) {
        ActivityLog exportLog = ActivityLog.builder()
                .organizationId(user.getOrganizationId()).userId(user.getUserId())
                .username(user.getUsername()).fullName(resolveCurrentUserName(user)).actorRole(user.getRoleCode())
                .action("EXPORT_ACTIVITY_LOG")
                .description("Xuất nhật ký hoạt động: từ ngày=" + valueOrDefault(request.getStartDate(), "toàn bộ")
                        + ", đến ngày=" + valueOrDefault(request.getEndDate(), "toàn bộ")
                        + ", hành động="
                        + valueOrDefault(ActivityLogExportLabelFormatter.formatAction(request.getAction()), "tất cả")
                        + ", người thực hiện=" + valueOrDefault(request.getActorName(), "tất cả")
                        + ", loại đối tượng="
                        + valueOrDefault(
                                ActivityLogExportLabelFormatter.formatObjectType(request.getObjectType()), "tất cả")
                        + ", số bản ghi=" + recordCount + ", trạng thái=" + formatExportStatus(status)
                        + ", mã yêu cầu=" + valueOrDefault(jobId, "không có"))
                .entityType("ACTIVITY_LOG_EXPORT").entityId(jobId == null ? null : jobId.toString())
                .createdAt(LocalDateTime.now(clock)).build();
        activityLogRepository.saveAndFlush(exportLog);
    }

    private String resolveActorName(ActivityLog log) {
        return log.getFullName() != null && !log.getFullName().isBlank() ? log.getFullName() : log.getUsername();
    }

    private String resolveCurrentUserName(CustomUserDetails user) {
        return user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getUsername();
    }

    private String valueOrDefault(Object value, String defaultValue) {
        return value == null || value.toString().isBlank() ? defaultValue : value.toString();
    }

    private String formatExportStatus(String status) {
        return switch (status) {
            case "SUCCESS" -> "thành công";
            case "IN_PROGRESS" -> "đang xử lý";
            case "FAILED" -> "thất bại";
            default -> status;
        };
    }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
