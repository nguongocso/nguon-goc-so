package vn.nguongocso.alert.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.dto.request.ActivityLogExportFilterRequest;
import vn.nguongocso.alert.dto.response.ActivityLogExportPreviewResponse;
import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.repository.ActivityLogRepository;
import vn.nguongocso.alert.service.ActivityLogExportService;
import vn.nguongocso.alert.specification.ActivityLogSpecification;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;

/**
 * Triển khai xuất nhật ký hoạt động theo bộ lọc của tổ chức hiện tại.
 */
@Service
@RequiredArgsConstructor
public class ActivityLogExportServiceImpl implements ActivityLogExportService {
    public static final int MAX_DIRECT_EXPORT_RECORDS = 10_000;
    private static final String ROLE_ORGANIZATION_MANAGER = "VT-02";
    private static final String DIRECT_MODE = "DIRECT";
    private static final String NULL_VALUE = "null";
    private static final String[] CSV_HEADERS = {
            "occurredAt",
            "actorName",
            "actorUsername",
            "actorRole",
            "actionType",
            "objectType",
            "objectIdentifier",
            "beforeValue",
            "afterValue"
    };

    private final ActivityLogRepository activityLogRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ActivityLogExportPreviewResponse preview(
            ActivityLogExportFilterRequest request,
            CustomUserDetails currentUser) {
        validateRequest(request, currentUser);
        long count = activityLogRepository.count(buildSpecification(request, currentUser));
        return ActivityLogExportPreviewResponse.builder()
                .count(count)
                .mode(DIRECT_MODE)
                .build();
    }

    @Override
    @Transactional
    public byte[] exportCsv(
            ActivityLogExportFilterRequest request,
            CustomUserDetails currentUser) {
        validateRequest(request, currentUser);

        Sort sort = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
        Pageable pageable = PageRequest.of(0, MAX_DIRECT_EXPORT_RECORDS + 1, sort);

        Page<ActivityLog> page = activityLogRepository.findAll(
                buildSpecification(request, currentUser),
                pageable);

        List<ActivityLog> snapshot = page.getContent();

        if (snapshot.isEmpty()) {
            throw new BusinessException("Không có nhật ký hoạt động trong phạm vi lọc.");
        }

        if (snapshot.size() > MAX_DIRECT_EXPORT_RECORDS) {
            throw new BusinessException(
                    "Số lượng bản ghi vượt quá giới hạn xuất trực tiếp (tối đa 10.000 bản ghi). Vui lòng thu hẹp khoảng thời gian hoặc điều kiện lọc.");
        }

        byte[] csv = createCsv(snapshot);
        saveExportAudit(request, currentUser, snapshot.size());
        return csv;
    }

    private Specification<ActivityLog> buildSpecification(
            ActivityLogExportFilterRequest request,
            CustomUserDetails currentUser) {
        return ActivityLogSpecification.hasOrganizationId(currentUser.getOrganizationId())
                .and(ActivityLogSpecification.createdBetween(request.getStartDate(), request.getEndDate()))
                .and(ActivityLogSpecification.hasAction(request.getAction()))
                .and(ActivityLogSpecification.hasActorName(request.getActorName()))
                .and(ActivityLogSpecification.hasEntityType(request.getObjectType()));
    }

    private void validateRequest(
            ActivityLogExportFilterRequest request,
            CustomUserDetails currentUser) {
        if (currentUser == null || !ROLE_ORGANIZATION_MANAGER.equals(currentUser.getRoleCode())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Chỉ Quản lý tổ chức (VT-02) mới được xuất nhật ký hoạt động.");
        }
        if (currentUser.getOrganizationId() == null) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Người dùng không thuộc tổ chức nào.");
        }
        if (request == null) {
            throw new BusinessException("Bộ lọc xuất nhật ký không được để trống.");
        }
        if (request.getStartDate() != null
                && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
    }

    private byte[] createCsv(List<ActivityLog> snapshot) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            output.write(0xEF);
            output.write(0xBB);
            output.write(0xBF);

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader(CSV_HEADERS)
                    .setRecordSeparator("\r\n")
                    .get();

            try (OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
                    CSVPrinter printer = new CSVPrinter(writer, format)) {
                for (ActivityLog log : snapshot) {
                    printer.printRecord(
                            log.getCreatedAt(),
                            protectFormula(resolveActorName(log)),
                            protectFormula(log.getUsername()),
                            NULL_VALUE,
                            protectFormula(log.getAction()),
                            protectFormula(log.getEntityType()),
                            protectFormula(log.getEntityId()),
                            NULL_VALUE,
                            NULL_VALUE);
                }
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể tạo tệp nhật ký hoạt động.");
        }
    }

    private void saveExportAudit(
            ActivityLogExportFilterRequest request,
            CustomUserDetails currentUser,
            int recordCount) {
        ActivityLog exportLog = ActivityLog.builder()
                .organizationId(currentUser.getOrganizationId())
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(resolveCurrentUserName(currentUser))
                .action("EXPORT_ACTIVITY_LOG")
                .description(buildAuditDescription(request, recordCount))
                .entityType("ACTIVITY_LOG_EXPORT")
                .ipAddress(null)
                .createdAt(LocalDateTime.now(clock))
                .build();
        activityLogRepository.saveAndFlush(exportLog);
    }

    private String buildAuditDescription(ActivityLogExportFilterRequest request, int recordCount) {
        return "Xuất nhật ký hoạt động: startDate=" + valueOrNull(request.getStartDate())
                + ", endDate=" + valueOrNull(request.getEndDate())
                + ", action=" + valueOrNull(request.getAction())
                + ", actorName=" + valueOrNull(request.getActorName())
                + ", objectType=" + valueOrNull(request.getObjectType())
                + ", recordCount=" + recordCount
                + ", status=SUCCESS";
    }

    private String resolveActorName(ActivityLog log) {
        return log.getFullName() != null && !log.getFullName().isBlank()
                ? log.getFullName()
                : log.getUsername();
    }

    private String resolveCurrentUserName(CustomUserDetails currentUser) {
        return currentUser.getFullName() != null && !currentUser.getFullName().isBlank()
                ? currentUser.getFullName()
                : currentUser.getUsername();
    }

    public String protectFormula(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String stripped = value.stripLeading();
        if (stripped.isEmpty()) {
            return value;
        }
        char first = stripped.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@'
                ? "'" + value
                : value;
    }

    private String valueOrNull(Object value) {
        return value == null || value.toString().isBlank() ? NULL_VALUE : value.toString();
    }
}
