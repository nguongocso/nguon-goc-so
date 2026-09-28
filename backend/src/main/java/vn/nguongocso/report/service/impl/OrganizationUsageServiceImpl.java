package vn.nguongocso.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.repository.ActivityLogRepository;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.report.dto.response.OrganizationUsageDashboardResponse;
import vn.nguongocso.report.dto.response.OrganizationUsageDashboardResponse.MetricComparison;
import vn.nguongocso.report.dto.response.OrganizationUsageDashboardResponse.OrganizationUsageItem;
import vn.nguongocso.report.pdf.OrganizationUsagePdfGenerator;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.report.service.OrganizationUsageService;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Triển khai dịch vụ tổng hợp mức độ sử dụng nền tảng. */
@Service
@RequiredArgsConstructor
public class OrganizationUsageServiceImpl implements OrganizationUsageService {

    /** Số ngày không có hoạt động thì đánh dấu cần liên hệ hỗ trợ. */
    private static final long INACTIVE_THRESHOLD_DAYS = 30;

    private static final DateTimeFormatter CSV_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrganizationRepository organizationRepository;
    private final ProductionLotRepository productionLotRepository;
    private final FarmLogRepository farmLogRepository;
    private final ChainEventRepository chainEventRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final TraceCodeScanLogRepository traceCodeScanLogRepository;
    private final ActivityLogRepository activityLogRepository;
    private final OrganizationUsagePdfGenerator organizationUsagePdfGenerator;
    private final Clock businessClock;

    /** Lấy dữ liệu mức độ sử dụng của từng tổ chức trong kỳ. */
    @Override
    @Transactional(readOnly = true)
    public OrganizationUsageDashboardResponse getDashboard(
        LocalDate startDate,
        LocalDate endDate,
        UUID organizationId) {

        ResolvedPeriod period = resolvePeriod(startDate, endDate);
        List<Organization> organizations = loadOrganizations(organizationId);

        Map<UUID, Long> lotsCurrent = toCountMap(productionLotRepository.countLotsGroupedByOrg(period.currentFrom(), period.currentTo()));
        Map<UUID, Long> lotsPrevious = toCountMap(productionLotRepository.countLotsGroupedByOrg(period.previousFrom(), period.previousTo()));
        Map<UUID, Long> logsCurrent = toCountMap(farmLogRepository.countFarmLogsGroupedByOrg(period.currentFrom(), period.currentTo()));
        Map<UUID, Long> logsPrevious = toCountMap(farmLogRepository.countFarmLogsGroupedByOrg(period.previousFrom(), period.previousTo()));
        Map<UUID, Long> eventsCurrent = mergeCounts(
            chainEventRepository.countEventsGroupedByShipmentOrg(period.currentFrom(), period.currentTo()),
            chainEventRepository.countUnassignedEventsGroupedByRecordedOrg(period.currentFrom(), period.currentTo()));
        Map<UUID, Long> eventsPrevious = mergeCounts(
            chainEventRepository.countEventsGroupedByShipmentOrg(period.previousFrom(), period.previousTo()),
            chainEventRepository.countUnassignedEventsGroupedByRecordedOrg(period.previousFrom(), period.previousTo()));
        Map<UUID, Long> labelsCurrent = toCountMap(traceCodeRepository.countActivatedGroupedByOrg(period.currentFrom(), period.currentTo()));
        Map<UUID, Long> labelsPrevious = toCountMap(traceCodeRepository.countActivatedGroupedByOrg(period.previousFrom(), period.previousTo()));
        Map<UUID, Long> lookupsCurrent = toCountMap(traceCodeScanLogRepository.countScansGroupedByOrg(period.currentFrom(), period.currentTo()));
        Map<UUID, Long> lookupsPrevious = toCountMap(traceCodeScanLogRepository.countScansGroupedByOrg(period.previousFrom(), period.previousTo()));
        Map<UUID, Long> usersCurrent = toCountMap(activityLogRepository.countDistinctUsersGroupedByOrg(period.currentFrom(), period.currentTo()));
        Map<UUID, Long> usersPrevious = toCountMap(activityLogRepository.countDistinctUsersGroupedByOrg(period.previousFrom(), period.previousTo()));

        Map<UUID, LocalDateTime> lastActivity = mergeMax(
            toTimeMap(productionLotRepository.maxLotCreatedAtGroupedByOrg()),
            toTimeMap(farmLogRepository.maxFarmLogCreatedAtGroupedByOrg()),
            toTimeMap(chainEventRepository.maxEventCreatedAtGroupedByShipmentOrg()),
            toTimeMap(chainEventRepository.maxUnassignedEventCreatedAtGroupedByRecordedOrg()),
            toTimeMap(traceCodeRepository.maxActivatedAtGroupedByOrg()),
            toTimeMap(traceCodeScanLogRepository.maxScannedAtGroupedByOrg()),
            toTimeMap(activityLogRepository.maxActivityAtGroupedByOrg()));

        LocalDateTime now = LocalDateTime.now(businessClock);
        LocalDateTime inactiveThreshold = now.minusDays(INACTIVE_THRESHOLD_DAYS);

        List<OrganizationUsageItem> items = new ArrayList<>();
        for (Organization org : organizations) {
            UUID orgId = org.getOrganizationId();
            MetricComparison lots = compare(lotsCurrent.getOrDefault(orgId, 0L), lotsPrevious.getOrDefault(orgId, 0L));
            MetricComparison logs = compare(logsCurrent.getOrDefault(orgId, 0L), logsPrevious.getOrDefault(orgId, 0L));
            MetricComparison events = compare(eventsCurrent.getOrDefault(orgId, 0L), eventsPrevious.getOrDefault(orgId, 0L));
            MetricComparison labels = compare(labelsCurrent.getOrDefault(orgId, 0L), labelsPrevious.getOrDefault(orgId, 0L));
            MetricComparison lookups = compare(lookupsCurrent.getOrDefault(orgId, 0L), lookupsPrevious.getOrDefault(orgId, 0L));
            MetricComparison users = compare(usersCurrent.getOrDefault(orgId, 0L), usersPrevious.getOrDefault(orgId, 0L));

            boolean hasData = hasAnyActivity(lots, logs, events, labels, lookups, users)
                && !org.getCreatedAt().isAfter(period.currentTo());
            LocalDateTime lastActivityAt = lastActivity.get(orgId);
            boolean needsSupport = lastActivityAt == null || !lastActivityAt.isAfter(inactiveThreshold);

            items.add(OrganizationUsageItem.builder()
                .organizationId(orgId)
                .organizationCode(org.getCode())
                .organizationName(org.getName())
                .organizationType(org.getType() != null ? org.getType().name() : null)
                .organizationStatus(org.getStatus() != null ? org.getStatus().name() : null)
                .createdAt(org.getCreatedAt())
                .hasData(hasData)
                .lastActivityAt(lastActivityAt)
                .needsSupport(needsSupport)
                .productionLots(lots)
                .farmLogs(logs)
                .chainEvents(events)
                .activatedLabels(labels)
                .publicLookups(lookups)
                .activeUsers(users)
                .build());
        }
        items.sort(Comparator.comparing(OrganizationUsageItem::getOrganizationName,
            Comparator.nullsLast(String::compareToIgnoreCase)));

        return OrganizationUsageDashboardResponse.builder()
            .startDate(period.start())
            .endDate(period.end())
            .previousStartDate(period.previousStart())
            .previousEndDate(period.previousEnd())
            .totalOrganizations(items.size())
            .items(items)
            .build();
    }

    /** Xuất báo cáo mức độ sử dụng theo kỳ ra file CSV. */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsv(
        LocalDate startDate,
        LocalDate endDate,
        UUID organizationId) {

        OrganizationUsageDashboardResponse dashboard = getDashboard(startDate, endDate, organizationId);
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("STT,Mã tổ chức,Tên tổ chức,Loại,Trạng thái,")
            .append("Lô sản xuất,Nhật ký,Sự kiện chuỗi,Tem kích hoạt,")
            .append("Tra cứu công khai,Người dùng HT,Hoạt động gần nhất\n");
        int stt = 1;
        for (OrganizationUsageItem item : dashboard.getItems()) {
            csv.append(stt++).append(',')
                .append(escape(item.getOrganizationCode())).append(',')
                .append(escape(item.getOrganizationName())).append(',')
                .append(escape(organizationTypeLabel(item.getOrganizationType()))).append(',')
                .append(escape(usageStatusLabel(item))).append(',')
                .append(escape(formatMetricAggregate(item.getProductionLots(), item.isHasData()))).append(',')
                .append(escape(formatMetricAggregate(item.getFarmLogs(), item.isHasData()))).append(',')
                .append(escape(formatMetricAggregate(item.getChainEvents(), item.isHasData()))).append(',')
                .append(escape(formatMetricAggregate(item.getActivatedLabels(), item.isHasData()))).append(',')
                .append(escape(formatMetricAggregate(item.getPublicLookups(), item.isHasData()))).append(',')
                .append(escape(formatMetricAggregate(item.getActiveUsers(), item.isHasData()))).append(',')
                .append(formatDateTime(item.getLastActivityAt())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Xuất báo cáo mức độ sử dụng theo kỳ ra file PDF. */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportPdf(
        LocalDate startDate,
        LocalDate endDate,
        UUID organizationId) {

        OrganizationUsageDashboardResponse dashboard = getDashboard(startDate, endDate, organizationId);
        return organizationUsagePdfGenerator.generate(dashboard);
    }

    /** Dịch loại tổ chức sang nhãn tiếng Việt. */
    private String organizationTypeLabel(String type) {
        if (type == null) {
            return "";
        }
        switch (type) {
        case "COOPERATIVE":
            return "Hợp tác xã";
        case "ENTERPRISE":
            return "Doanh nghiệp";
        case "GOVERNMENT":
            return "Cơ quan quản lý";
        case "SYSTEM":
            return "Tổ chức hệ thống";
        default:
            return type;
        }
    }

    /** Lấy nhãn trạng thái sử dụng của tổ chức. */
    private String usageStatusLabel(OrganizationUsageItem item) {
        if (item.isNeedsSupport() && item.getLastActivityAt() != null) {
            return "Cần liên hệ hỗ trợ";
        }
        if (item.isHasData()) {
            return "Đang hoạt động";
        }
        return "Chưa có dữ liệu";
    }

    /** Chuẩn hóa kỳ hiện tại và kỳ đối chiếu trước đó. */
    private ResolvedPeriod resolvePeriod(
        LocalDate startDate,
        LocalDate endDate) {

        LocalDate today = LocalDate.now(businessClock);
        LocalDate start = startDate != null ? startDate : today.minusDays(29);
        LocalDate end = endDate != null ? endDate : today;
        if (start.isAfter(end)) {
            throw new BusinessException("Khoảng thời gian không hợp lệ: từ ngày phải trước hoặc bằng đến ngày.");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate previousEnd = start.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(days - 1);
        return new ResolvedPeriod(
            start, end, previousStart, previousEnd,
            start.atStartOfDay(), end.atTime(LocalTime.of(23, 59, 59)),
            previousStart.atStartOfDay(), previousEnd.atTime(LocalTime.of(23, 59, 59)));
    }

    /** Tải danh sách tổ chức theo điều kiện lọc. */
    private List<Organization> loadOrganizations(UUID organizationId) {
        if (organizationId != null) {
            return organizationRepository.findById(organizationId)
                .map(List::of)
                .orElse(List.of());
        }
        return organizationRepository.findAll();
    }

    /** So sánh chỉ số hoạt động giữa hai kỳ. */
    MetricComparison compare(
        long current,
        long previous) {

        long change = current - previous;
        Double changePercent = previous == 0 ? null : (change * 100.0 / previous);
        return MetricComparison.builder()
            .current(current)
            .previous(previous)
            .change(change)
            .changePercent(changePercent)
            .build();
    }

    /** Kiểm tra tổ chức có hoạt động trong kỳ hay không. */
    private boolean hasAnyActivity(MetricComparison... metrics) {
        for (MetricComparison metric : metrics) {
            if (metric.getCurrent() > 0 || metric.getPrevious() > 0) {
                return true;
            }
        }
        return false;
    }

    /** Chuyển đổi danh sách kết quả đếm sang bản đồ theo ID tổ chức. */
    private Map<UUID, Long> toCountMap(List<Object[]> rows) {
        Map<UUID, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    /** Chuyển đổi danh sách thời gian sang bản đồ theo ID tổ chức. */
    private Map<UUID, LocalDateTime> toTimeMap(List<Object[]> rows) {
        Map<UUID, LocalDateTime> result = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null && row[1] != null) {
                result.put((UUID) row[0], (LocalDateTime) row[1]);
            }
        }
        return result;
    }

    /** Hợp nhất các bản đồ đếm số lượng. */
    @SafeVarargs
    private Map<UUID, Long> mergeCounts(List<Object[]>... groups) {
        Map<UUID, Long> result = new HashMap<>();
        for (List<Object[]> rows : groups) {
            for (Object[] row : rows) {
                result.merge((UUID) row[0], ((Number) row[1]).longValue(), Long::sum);
            }
        }
        return result;
    }

    /** Hợp nhất các bản đồ thời gian lấy thời điểm mới nhất. */
    @SafeVarargs
    private Map<UUID, LocalDateTime> mergeMax(Map<UUID, LocalDateTime>... maps) {
        Map<UUID, LocalDateTime> result = new HashMap<>();
        for (Map<UUID, LocalDateTime> map : maps) {
            for (Map.Entry<UUID, LocalDateTime> entry : map.entrySet()) {
                result.merge(entry.getKey(), entry.getValue(),
                    (oldTime, newTime) -> oldTime.isAfter(newTime) ? oldTime : newTime);
            }
        }
        return result;
    }

    /** Định dạng chỉ số tổng hợp cho xuất file. */
    private String formatMetricAggregate(
        MetricComparison metric,
        boolean hasData) {

        if (!hasData || metric == null) {
            return "—";
        }
        double percent = metric.getChangePercent() != null
            ? metric.getChangePercent()
            : (metric.getChange() > 0 ? 100.0 : 0.0);
        return String.format("%d (%+.1f%%)", metric.getCurrent(), percent);
    }

    /** Lọc ký tự đặc biệt cho file CSV. */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\"", "\"\"");
        if (normalized.contains(",") || normalized.contains("\"") || normalized.contains("\n")) {
            return "\"" + normalized + "\"";
        }
        return normalized;
    }

    /** Định dạng ngày giờ hiển thị. */
    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(CSV_DATETIME) : "";
    }

    /** Kỳ hiện tại và kỳ trước đã chuẩn hóa kèm mốc thời gian truy vấn. */
    private record ResolvedPeriod(
        LocalDate start,
        LocalDate end,
        LocalDate previousStart,
        LocalDate previousEnd,
        LocalDateTime currentFrom,
        LocalDateTime currentTo,
        LocalDateTime previousFrom,
        LocalDateTime previousTo) {
    }
}
