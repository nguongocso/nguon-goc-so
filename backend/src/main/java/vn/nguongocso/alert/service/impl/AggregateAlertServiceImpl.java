package vn.nguongocso.alert.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.dto.response.AggregateAlertCountResponse;
import vn.nguongocso.alert.dto.response.AggregateAlertItemResponse;
import vn.nguongocso.alert.dto.response.AggregateAlertPageResponse;
import vn.nguongocso.alert.dto.response.UnviewedAlertCountResponse;
import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.alert.enums.AlertSeverity;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.enums.AggregateAlertType;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.alert.service.AggregateAlertService;
import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.MilestoneReminder;
import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.farm.enums.MilestoneReminderStatus;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;
import vn.nguongocso.farm.repository.MilestoneReminderRepository;
import vn.nguongocso.farm.repository.ProductFeedbackRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.recall.entity.RecallCase;
import vn.nguongocso.trace.recall.enums.RecallCaseStatus;
import vn.nguongocso.trace.recall.repository.RecallCaseRepository;
import vn.nguongocso.trace.repository.CodeRangeRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Triển khai dịch vụ tổng hợp cảnh báo gom từ 7 nguồn dữ liệu (NCL-08-CN-016).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregateAlertServiceImpl implements AggregateAlertService {

    private static final String ROLE_ADMIN = "VT-01";
    private static final String ROLE_COOP_MANAGER = "VT-02";

    private final AlertRepository alertRepository;
    private final ProductFeedbackRepository productFeedbackRepository;
    private final CodeRangeRepository codeRangeRepository;
    private final MilestoneReminderRepository milestoneReminderRepository;
    private final RecallCaseRepository recallCaseRepository;
    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public AggregateAlertPageResponse getAggregateAlerts(
            String type,
            String severity,
            String status,
            UUID organizationId,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        UUID targetOrgId = resolveTargetOrganizationId(organizationId);
        String effectiveStatus = (status == null || status.isBlank()) ? "OPEN" : status.toUpperCase();

        // 1. Gom tất cả các mục từ 7 nguồn theo tổ chức và trạng thái
        List<AggregateAlertItemResponse> allItems = collectAllAlerts(targetOrgId, effectiveStatus);

        // 2. Thống kê theo mức khẩn cấp và loại trước khi lọc chi tiết
        AggregateAlertCountResponse summaryCounts = calculateSummaryCounts(allItems);

        // 3. Lọc theo tiêu chí người dùng yêu cầu
        List<AggregateAlertItemResponse> filteredItems = allItems.stream()
                .filter(item -> filterByType(item, type))
                .filter(item -> filterBySeverity(item, severity))
                .filter(item -> filterByKeyword(item, keyword))
                .filter(item -> filterByDateRange(item, fromDate, toDate))
                .toList();

        // 4. Sắp xếp: Ưu tiên mức khẩn cấp HIGH trước MEDIUM, sau đó thời gian tạo mới nhất trước
        List<AggregateAlertItemResponse> sortedItems = new ArrayList<>(filteredItems);
        sortedItems.sort((a, b) -> {
            int severityCompare = Integer.compare(getSeverityWeight(b.getSeverity()), getSeverityWeight(a.getSeverity()));
            if (severityCompare != 0) {
                return severityCompare;
            }
            LocalDateTime dateA = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
            LocalDateTime dateB = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
            return dateB.compareTo(dateA);
        });

        // 5. Phân trang
        int totalElements = sortedItems.size();
        int pageSize = pageable.getPageSize() > 0 ? pageable.getPageSize() : 10;
        int currentPage = Math.max(0, pageable.getPageNumber());
        int fromIndex = Math.min(currentPage * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<AggregateAlertItemResponse> pageContent = sortedItems.subList(fromIndex, toIndex);
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        return AggregateAlertPageResponse.builder()
                .items(pageContent)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .currentPage(currentPage)
                .pageSize(pageSize)
                .summaryCounts(summaryCounts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AggregateAlertCountResponse getAggregateAlertCounts(UUID organizationId) {
        UUID targetOrgId = resolveTargetOrganizationId(organizationId);
        List<AggregateAlertItemResponse> openItems = collectAllAlerts(targetOrgId, "OPEN");
        return calculateSummaryCounts(openItems);
    }

    @Override
    @Transactional(readOnly = true)
    public UnviewedAlertCountResponse getUnviewedAlertCount() {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        String roleCode = currentUser.getRoleCode();

        UUID targetOrgId = ROLE_ADMIN.equals(roleCode) ? null : currentUser.getOrganizationId();
        List<AggregateAlertItemResponse> openItems = collectAllAlerts(targetOrgId, "OPEN");

        long count = openItems.size();
        boolean hasHigh = openItems.stream().anyMatch(item -> item.getSeverity() == AlertSeverity.HIGH);

        return UnviewedAlertCountResponse.builder()
                .unviewedCount(count)
                .hasHighSeverity(hasHigh)
                .build();
    }

    /**
     * Xác định organizationId hợp lệ dựa theo vai trò của người dùng và quy tắc QTN-01.
     */
    private UUID resolveTargetOrganizationId(UUID requestedOrgId) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        String roleCode = currentUser.getRoleCode();

        if (ROLE_ADMIN.equals(roleCode)) {
            // Quản trị viên hệ thống có quyền xem toàn bộ hoặc lọc theo tổ chức bất kỳ
            return requestedOrgId;
        } else if (ROLE_COOP_MANAGER.equals(roleCode)) {
            UUID userOrgId = currentUser.getOrganizationId();
            if (requestedOrgId != null && !requestedOrgId.equals(userOrgId)) {
                log.warn("Cảnh báo truy cập trái phép QTN-01: User {} thuộc org {} cố truy cập org {}",
                        currentUser.getUsername(), userOrgId, requestedOrgId);
                throw new BusinessException("Bạn không có quyền xem cảnh báo của tổ chức khác.");
            }
            return userOrgId;
        } else {
            throw new BusinessException("Bạn không có quyền xem cảnh báo tổng hợp.");
        }
    }

    /**
     * Thu thập cảnh báo từ 7 nguồn dữ liệu.
     */
    private List<AggregateAlertItemResponse> collectAllAlerts(UUID orgId, String statusFilter) {
        List<AggregateAlertItemResponse> result = new ArrayList<>();

        boolean includeOpen = "OPEN".equalsIgnoreCase(statusFilter) || "ALL".equalsIgnoreCase(statusFilter);
        boolean includeResolved = "RESOLVED".equalsIgnoreCase(statusFilter) || "ALL".equalsIgnoreCase(statusFilter);

        // Nguồn 1, 2, 3: Bảng alerts (Tem quét bất thường, Chứng nhận, Kiểm nghiệm)
        collectAlertsFromAlertTable(result, orgId, includeOpen, includeResolved);

        // Nguồn 4: Phản ánh của người tiêu dùng chưa xử lý
        collectProductFeedbackAlerts(result, orgId, includeOpen, includeResolved);

        // Nguồn 5: Hạn mức dải mã truy xuất sắp hết
        if (includeOpen) {
            collectCodeRangeQuotaAlerts(result, orgId);
        }

        // Nguồn 6: Mốc canh tác quá hạn ghi nhật ký
        collectMilestoneReminderAlerts(result, orgId, includeOpen, includeResolved);

        // Nguồn 7: Vụ việc thu hồi đang mở
        collectRecallCaseAlerts(result, orgId, includeOpen, includeResolved);

        return result;
    }

    /**
     * Gom cảnh báo từ bảng alerts (Nguồn 1, 2, 3).
     */
    private void collectAlertsFromAlertTable(
            List<AggregateAlertItemResponse> result,
            UUID orgId,
            boolean includeOpen,
            boolean includeResolved) {

        List<Alert> alerts = new ArrayList<>();
        if (includeOpen) {
            if (orgId != null) {
                alerts.addAll(alertRepository.findByOrganizationOrganizationIdAndStatus(orgId, AlertStatus.PENDING));
            } else {
                alerts.addAll(alertRepository.findByStatus(AlertStatus.PENDING));
            }
        }
        if (includeResolved) {
            if (orgId != null) {
                alerts.addAll(alertRepository.findByOrganizationOrganizationIdAndStatus(orgId, AlertStatus.RESOLVED));
            } else {
                alerts.addAll(alertRepository.findByStatus(AlertStatus.RESOLVED));
            }
        }

        for (Alert alert : alerts) {
            AggregateAlertType aggType = mapAlertTypeToAggregate(alert.getType());
            String actionUrl;
            String defaultTitle;

            switch (alert.getType()) {
                case SCAN_ANOMALY:
                    actionUrl = "/alerts/scan-anomaly";
                    defaultTitle = "Tem quét bất thường cần xác minh";
                    break;
                case CERT_EXPIRING:
                    actionUrl = "/certifications";
                    defaultTitle = "Chứng nhận sắp hết hạn hiệu lực";
                    break;
                case CERT_EXPIRED:
                    actionUrl = "/certifications";
                    defaultTitle = "Chứng nhận đã hết hiệu lực";
                    break;
                case INSPECTION_EXPIRING:
                    actionUrl = "/production-lots/" + alert.getRelatedEntityId();
                    defaultTitle = "Kết quả kiểm nghiệm sắp hết hiệu lực";
                    break;
                case INSPECTION_EXPIRED:
                    actionUrl = "/production-lots/" + alert.getRelatedEntityId();
                    defaultTitle = "Kết quả kiểm nghiệm đã hết hiệu lực";
                    break;
                default:
                    actionUrl = "/alerts";
                    defaultTitle = "Cảnh báo hệ thống";
            }

            String entityName = extractEntityNameFromDetails(alert.getDetails());

            result.add(AggregateAlertItemResponse.builder()
                    .id(alert.getId())
                    .type(aggType)
                    .typeName(aggType.getDisplayName())
                    .severity(alert.getSeverity())
                    .title(defaultTitle)
                    .message(alert.getMessage() != null ? alert.getMessage() : defaultTitle)
                    .relatedEntityType(alert.getRelatedEntityType())
                    .relatedEntityId(alert.getRelatedEntityId())
                    .relatedEntityName(entityName)
                    .createdAt(alert.getCreatedAt())
                    .actionUrl(actionUrl)
                    .organizationId(alert.getOrganization() != null ? alert.getOrganization().getOrganizationId() : null)
                    .organizationName(alert.getOrganization() != null ? alert.getOrganization().getName() : "")
                    .status(alert.getStatus() == AlertStatus.PENDING ? "OPEN" : "RESOLVED")
                    .build());
        }
    }

    /**
     * Gom cảnh báo phản ánh chưa xử lý (Nguồn 4).
     */
    private void collectProductFeedbackAlerts(
            List<AggregateAlertItemResponse> result,
            UUID orgId,
            boolean includeOpen,
            boolean includeResolved) {

        List<ProductFeedbackStatus> openStatuses = List.of(ProductFeedbackStatus.NEW, ProductFeedbackStatus.IN_PROGRESS);
        List<ProductFeedback> feedbacks = new ArrayList<>();

        if (includeOpen) {
            if (orgId != null) {
                feedbacks.addAll(productFeedbackRepository.findByProductionLot_Organization_OrganizationIdAndStatusIn(orgId, openStatuses));
            } else {
                feedbacks.addAll(productFeedbackRepository.findByStatusIn(openStatuses));
            }
        }
        if (includeResolved) {
            List<ProductFeedbackStatus> closedStatuses = List.of(ProductFeedbackStatus.CLOSED, ProductFeedbackStatus.ESCALATED_TO_RECALL);
            if (orgId != null) {
                feedbacks.addAll(productFeedbackRepository.findByProductionLot_Organization_OrganizationIdAndStatusIn(orgId, closedStatuses));
            } else {
                feedbacks.addAll(productFeedbackRepository.findByStatusIn(closedStatuses));
            }
        }

        for (ProductFeedback pf : feedbacks) {
            boolean isCritical = pf.getSeverity() == ProductFeedbackSeverity.QUALITY_SUSPECTED
                    || pf.getSeverity() == ProductFeedbackSeverity.COUNTERFEIT_SUSPECTED;
            AlertSeverity severity = isCritical ? AlertSeverity.HIGH : AlertSeverity.MEDIUM;

            String statusStr = (pf.getStatus() == ProductFeedbackStatus.CLOSED || pf.getStatus() == ProductFeedbackStatus.ESCALATED_TO_RECALL)
                    ? "RESOLVED" : "OPEN";

            String lotName = (pf.getProductionLot() != null) ? pf.getProductionLot().getName() : "Lô sản xuất";
            Organization org = (pf.getProductionLot() != null) ? pf.getProductionLot().getOrganization() : null;

            result.add(AggregateAlertItemResponse.builder()
                    .id(pf.getId())
                    .type(AggregateAlertType.UNPROCESSED_FEEDBACK)
                    .typeName(AggregateAlertType.UNPROCESSED_FEEDBACK.getDisplayName())
                    .severity(severity)
                    .title("Phản ánh của người tiêu dùng (" + (pf.getStatus() == ProductFeedbackStatus.NEW ? "Mới" : "Đang xử lý") + ")")
                    .message(pf.getContent())
                    .relatedEntityType("PRODUCT_FEEDBACK")
                    .relatedEntityId(pf.getId())
                    .relatedEntityName(lotName)
                    .createdAt(pf.getCreatedAt())
                    .actionUrl("/product-feedbacks")
                    .organizationId(org != null ? org.getOrganizationId() : null)
                    .organizationName(org != null ? org.getName() : "")
                    .status(statusStr)
                    .build());
        }
    }

    /**
     * Gom cảnh báo hạn mức dải mã truy xuất (Nguồn 5).
     */
    private void collectCodeRangeQuotaAlerts(List<AggregateAlertItemResponse> result, UUID orgId) {
        List<CodeRange> codeRanges = (orgId != null)
                ? codeRangeRepository.findByOrganizationOrganizationId(orgId)
                : codeRangeRepository.findAll();

        for (CodeRange cr : codeRanges) {
            if (cr.getTotalLimit() == null || cr.getTotalLimit() <= 0) {
                continue;
            }
            long used = cr.getUsedCount() != null ? cr.getUsedCount() : 0L;
            double percent = (double) used / cr.getTotalLimit() * 100;

            if (percent >= 80.0) {
                boolean isExhausted = percent >= 100.0;
                AlertSeverity severity = isExhausted ? AlertSeverity.HIGH : AlertSeverity.MEDIUM;
                String title = isExhausted ? "Dải mã truy xuất đã hết hạn mức" : "Dải mã truy xuất sắp hết hạn mức";
                String message = String.format("Dải mã %s đã sử dụng %d/%d mã (%.1f%%). Vui lòng gửi yêu cầu cấp bổ sung mã truy xuất.",
                        cr.getPrefix(), used, cr.getTotalLimit(), percent);

                Organization org = cr.getOrganization();

                result.add(AggregateAlertItemResponse.builder()
                        .id(cr.getId())
                        .type(AggregateAlertType.CODE_RANGE_QUOTA)
                        .typeName(AggregateAlertType.CODE_RANGE_QUOTA.getDisplayName())
                        .severity(severity)
                        .title(title)
                        .message(message)
                        .relatedEntityType("CODE_RANGE")
                        .relatedEntityId(cr.getId())
                        .relatedEntityName(cr.getPrefix())
                        .createdAt(cr.getUpdatedAt() != null ? cr.getUpdatedAt() : cr.getCreatedAt())
                        .actionUrl("/code-range-supplements/create")
                        .organizationId(org != null ? org.getOrganizationId() : null)
                        .organizationName(org != null ? org.getName() : "")
                        .status("OPEN")
                        .build());
            }
        }
    }

    /**
     * Gom cảnh báo mốc canh tác quá hạn ghi nhật ký (Nguồn 6).
     */
    private void collectMilestoneReminderAlerts(
            List<AggregateAlertItemResponse> result,
            UUID orgId,
            boolean includeOpen,
            boolean includeResolved) {

        List<MilestoneReminder> reminders = new ArrayList<>();
        if (includeOpen) {
            if (orgId != null) {
                reminders.addAll(milestoneReminderRepository
                        .findByProductionLot_Organization_OrganizationIdAndStatusOrderByOverdueDaysDesc(orgId, MilestoneReminderStatus.OPEN));
            } else {
                reminders.addAll(milestoneReminderRepository.findByStatus(MilestoneReminderStatus.OPEN, Pageable.unpaged()).getContent());
            }
        }
        if (includeResolved) {
            if (orgId != null) {
                reminders.addAll(milestoneReminderRepository
                        .findByProductionLot_Organization_OrganizationIdAndStatusOrderByOverdueDaysDesc(orgId, MilestoneReminderStatus.COMPLETED));
            } else {
                reminders.addAll(milestoneReminderRepository.findByStatus(MilestoneReminderStatus.COMPLETED, Pageable.unpaged()).getContent());
            }
        }

        for (MilestoneReminder mr : reminders) {
            int overdueDays = mr.getOverdueDays() != null ? mr.getOverdueDays() : 0;
            AlertSeverity severity = overdueDays >= 7 ? AlertSeverity.HIGH : AlertSeverity.MEDIUM;

            String milestoneName = (mr.getMilestone() != null) ? mr.getMilestone().getName() : "Mốc bắt buộc";
            String lotName = (mr.getProductionLot() != null) ? mr.getProductionLot().getName() : "Lô sản xuất";
            UUID lotId = (mr.getProductionLot() != null) ? mr.getProductionLot().getId() : null;
            Organization org = (mr.getProductionLot() != null) ? mr.getProductionLot().getOrganization() : null;

            String title = "Quá hạn mốc canh tác: " + milestoneName;
            String message = String.format("Lô sản xuất '%s' đã quá hạn mốc canh tác bắt buộc '%s' %d ngày.",
                    lotName, milestoneName, overdueDays);

            result.add(AggregateAlertItemResponse.builder()
                    .id(mr.getId())
                    .type(AggregateAlertType.OVERDUE_MILESTONE)
                    .typeName(AggregateAlertType.OVERDUE_MILESTONE.getDisplayName())
                    .severity(severity)
                    .title(title)
                    .message(message)
                    .relatedEntityType("MILESTONE_REMINDER")
                    .relatedEntityId(mr.getId())
                    .relatedEntityName(lotName)
                    .createdAt(mr.getCreatedAt())
                    .actionUrl(lotId != null ? "/farm-logs/create?lotId=" + lotId : "/farm-logs/create")
                    .organizationId(org != null ? org.getOrganizationId() : null)
                    .organizationName(org != null ? org.getName() : "")
                    .status(mr.getStatus() == MilestoneReminderStatus.OPEN ? "OPEN" : "RESOLVED")
                    .build());
        }
    }

    /**
     * Gom cảnh báo vụ việc thu hồi đang mở (Nguồn 7).
     */
    private void collectRecallCaseAlerts(
            List<AggregateAlertItemResponse> result,
            UUID orgId,
            boolean includeOpen,
            boolean includeResolved) {

        List<RecallCase> cases = new ArrayList<>();
        if (includeOpen) {
            if (orgId != null) {
                cases.addAll(recallCaseRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(orgId, RecallCaseStatus.OPEN));
            } else {
                cases.addAll(recallCaseRepository.findByStatusOrderByCreatedAtDesc(RecallCaseStatus.OPEN));
            }
        }
        if (includeResolved) {
            if (orgId != null) {
                cases.addAll(recallCaseRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(orgId, RecallCaseStatus.CLOSED));
            } else {
                cases.addAll(recallCaseRepository.findByStatusOrderByCreatedAtDesc(RecallCaseStatus.CLOSED));
            }
        }

        // Cache tên tổ chức cho vụ việc thu hồi
        Map<UUID, String> orgNameCache = new HashMap<>();

        for (RecallCase rc : cases) {
            String orgName = orgNameCache.computeIfAbsent(rc.getOrganizationId(), id -> {
                if (rc.getProductionLot() != null && rc.getProductionLot().getOrganization() != null) {
                    return rc.getProductionLot().getOrganization().getName();
                }
                return organizationRepository.findById(id).map(Organization::getName).orElse("");
            });

            String lotName = rc.getProductionLot() != null ? rc.getProductionLot().getName() : "Lô sản xuất";

            result.add(AggregateAlertItemResponse.builder()
                    .id(rc.getId())
                    .type(AggregateAlertType.OPEN_RECALL_CASE)
                    .typeName(AggregateAlertType.OPEN_RECALL_CASE.getDisplayName())
                    .severity(AlertSeverity.HIGH) // Vụ việc thu hồi luôn ở mức khẩn cấp HIGH
                    .title("Vụ việc thu hồi đang mở: " + rc.getCaseCode())
                    .message(String.format("Vụ việc thu hồi %s đối với lô '%s' đang mở và cần xử lý dứt điểm các lô hàng liên quan.",
                            rc.getCaseCode(), lotName))
                    .relatedEntityType("RECALL_CASE")
                    .relatedEntityId(rc.getId())
                    .relatedEntityName(rc.getCaseCode())
                    .createdAt(rc.getCreatedAt())
                    .actionUrl("/recall-cases/" + rc.getId())
                    .organizationId(rc.getOrganizationId())
                    .organizationName(orgName)
                    .status(rc.getStatus() == RecallCaseStatus.OPEN ? "OPEN" : "RESOLVED")
                    .build());
        }
    }

    /**
     * Map AlertType từ bảng alerts sang AggregateAlertType.
     */
    private AggregateAlertType mapAlertTypeToAggregate(AlertType type) {
        if (type == null) {
            return AggregateAlertType.SCAN_ANOMALY;
        }
        return switch (type) {
            case CERT_EXPIRING -> AggregateAlertType.CERT_EXPIRING;
            case CERT_EXPIRED -> AggregateAlertType.CERT_EXPIRED;
            case INSPECTION_EXPIRING -> AggregateAlertType.INSPECTION_EXPIRING;
            case INSPECTION_EXPIRED -> AggregateAlertType.INSPECTION_EXPIRED;
            case SCAN_ANOMALY -> AggregateAlertType.SCAN_ANOMALY;
        };
    }

    /**
     * Trích xuất tên thực thể từ chuỗi JSON details của Alert.
     */
    private String extractEntityNameFromDetails(String detailsJson) {
        if (detailsJson == null || detailsJson.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(detailsJson);
            if (node.hasNonNull("certificationName")) {
                return node.get("certificationName").asText();
            }
            if (node.hasNonNull("lotName")) {
                return node.get("lotName").asText();
            }
            if (node.hasNonNull("traceCode")) {
                return node.get("traceCode").asText();
            }
        } catch (Exception ignored) {
            // Fallback khi parse lỗi
        }
        return null;
    }

    /**
     * Tính toán số lượng thống kê.
     */
    private AggregateAlertCountResponse calculateSummaryCounts(List<AggregateAlertItemResponse> items) {
        long highCount = 0;
        long mediumCount = 0;
        Map<String, Long> byType = new HashMap<>();

        for (AggregateAlertType type : AggregateAlertType.values()) {
            byType.put(type.name(), 0L);
        }

        for (AggregateAlertItemResponse item : items) {
            if ("OPEN".equals(item.getStatus())) {
                if (item.getSeverity() == AlertSeverity.HIGH) {
                    highCount++;
                } else {
                    mediumCount++;
                }
                String typeKey = item.getType() != null ? item.getType().name() : "";
                byType.put(typeKey, byType.getOrDefault(typeKey, 0L) + 1);
            }
        }

        return AggregateAlertCountResponse.builder()
                .totalOpen(highCount + mediumCount)
                .highSeverityCount(highCount)
                .mediumSeverityCount(mediumCount)
                .byTypeCounts(byType)
                .build();
    }

    private boolean filterByType(AggregateAlertItemResponse item, String type) {
        if (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type)) {
            return true;
        }
        return item.getType() != null && item.getType().name().equalsIgnoreCase(type.trim());
    }

    private boolean filterBySeverity(AggregateAlertItemResponse item, String severity) {
        if (severity == null || severity.isBlank() || "ALL".equalsIgnoreCase(severity)) {
            return true;
        }
        return item.getSeverity() != null && item.getSeverity().name().equalsIgnoreCase(severity.trim());
    }

    private boolean filterByKeyword(AggregateAlertItemResponse item, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String kw = keyword.trim().toLowerCase();
        return (item.getTitle() != null && item.getTitle().toLowerCase().contains(kw))
                || (item.getMessage() != null && item.getMessage().toLowerCase().contains(kw))
                || (item.getRelatedEntityName() != null && item.getRelatedEntityName().toLowerCase().contains(kw))
                || (item.getOrganizationName() != null && item.getOrganizationName().toLowerCase().contains(kw));
    }

    private boolean filterByDateRange(AggregateAlertItemResponse item, LocalDate fromDate, LocalDate toDate) {
        if (item.getCreatedAt() == null) {
            return true;
        }
        LocalDate itemDate = item.getCreatedAt().toLocalDate();
        if (fromDate != null && itemDate.isBefore(fromDate)) {
            return false;
        }
        if (toDate != null && itemDate.isAfter(toDate)) {
            return false;
        }
        return true;
    }

    private int getSeverityWeight(AlertSeverity severity) {
        if (severity == AlertSeverity.HIGH) {
            return 2;
        }
        return 1;
    }
}
