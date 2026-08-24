package vn.nguongocso.alert.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.alert.dto.request.ActivityLogRequest;
import vn.nguongocso.alert.dto.response.ActivityLogResponse;
import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.repository.ActivityLogRepository;
import vn.nguongocso.alert.specification.ActivityLogSpecification;
import vn.nguongocso.alert.service.ActivityLogService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/** Tra cứu và ghi nhật ký hoạt động. */
public class ActivityLogServiceImpl implements ActivityLogService {
    private final ActivityLogRepository activityLogRepository;

    /** Lấy danh sách nhật ký theo bộ lọc. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ActivityLogResponse> getActivityLogs(
            int page, int size, String action, String actorName,
            LocalDate startDate, LocalDate endDate, CustomUserDetails currentUser) {

        // Sắp xếp mặc định theo thời gian giảm dần (mới nhất trước)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Specification<ActivityLog> spec = ActivityLogSpecification.hasOrganizationId(currentUser.getOrganizationId());

        if (action != null && !action.isBlank()) {
            spec = spec.and(ActivityLogSpecification.hasAction(action));
        }
        if (actorName != null && !actorName.isBlank()) {
            spec = spec.and(ActivityLogSpecification.hasActorName(actorName));
        }
        if (startDate != null || endDate != null) {
            spec = spec.and(ActivityLogSpecification.createdBetween(startDate, endDate));
        }

        Page<ActivityLog> logPage = activityLogRepository.findAll(spec, pageable);

        List<ActivityLogResponse> items = logPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResponse.from(logPage, items);
    }

    /** Chuyển entity nhật ký sang response. */
    private ActivityLogResponse convertToResponse(ActivityLog log) {
        String actorName = log.getFullName() != null && !log.getFullName().isBlank()
                ? log.getFullName()
                : log.getUsername();

        return ActivityLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(log.getUsername())
                .fullName(log.getFullName())
                .actorName(actorName)
                .action(log.getAction())
                .actionType(log.getAction())
                .description(log.getDescription())
                .entityType(log.getEntityType())
                .targetType(log.getEntityType())
                .entityId(log.getEntityId())
                .targetId(log.getEntityId())
                .ipAddress(log.getIpAddress())
                .details(log.getDescription())
                .createdAt(log.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    /** Ghi một nhật ký hoạt động mới. */
    public void logActivity(ActivityLogRequest request) {

        ActivityLog activityLog = ActivityLog.builder()
                .organizationId(request.getOrganizationId())
                .userId(request.getUserId())
                .username(request.getUsername())
                .fullName(request.getFullName())
                .action(request.getAction())
                .description(request.getDescription())
                .entityType(request.getEntityType())
                .entityId(
                        request.getEntityId() == null
                                ? null
                                : request.getEntityId().toString())
                .ipAddress(request.getIpAddress())
                .build();

        activityLogRepository.save(activityLog);
    }
}
