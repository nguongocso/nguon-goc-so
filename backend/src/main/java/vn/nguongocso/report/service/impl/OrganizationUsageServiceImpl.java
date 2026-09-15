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

/**
 * Service tổng hợp mức độ sử dụng nền tảng theo từng tổ chức (NCL-07-CN-008).
 *
 * <p>Nguồn số liệu của 6 chỉ số (xác minh từ entity/repository hiện có):</p>
 * <ul>
 *   <li>Lô sản xuất tạo mới: bảng {@code production_lot} theo {@code created_at}.</li>
 *   <li>Mục nhật ký: bảng {@code farm_logs} theo {@code created_at}, tổ chức suy ra qua lô sản xuất.</li>
 *   <li>Sự kiện chuỗi: bảng {@code chain_events} (loại trừ đính chính) theo {@code created_at},
 *       tổ chức sở hữu lô hàng, cộng thêm sự kiện chưa gắn lô hàng theo tổ chức đã ghi.</li>
 *   <li>Tem kích hoạt: bảng {@code trace_codes} theo {@code activated_at}
 *       (không lọc theo status hiện tại vì tem còn chuyển trạng thái sau kích hoạt).</li>
 *   <li>Lượt tra cứu công khai: bảng {@code trace_code_scan_logs} theo {@code scanned_at}.</li>
 *   <li>Người dùng hoạt động: userId phân biệt trong {@code activity_logs}
 *       (người dùng có hoạt động thật, không phải tổng số tài khoản).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class OrganizationUsageServiceImpl implements OrganizationUsageService {

    /** Số ngày không có hoạt động thì đánh dấu cần liên hệ hỗ trợ. */
    private static final long INACTIVE_THRESHOLD_DAYS = 30;

    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter CSV_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrganizationRepository organizationRepository;
    private final ProductionLotRepository productionLotRepository;
    private final FarmLogRepository farmLogRepository;
    private final ChainEventRepository chainEventRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final TraceCodeScanLogRepository traceCodeScanLogRepository;
    private final ActivityLogRepository activityLogRepository;
    private final Clock businessClock;

    /**
     * Lấy dữ liệu mức độ sử dụng của từng tổ chức trong kỳ.
     */
    @Override
    @Transactional(readOnly = true)
    public OrganizationUsageDashboardResponse getDashboard(LocalDate startDate, LocalDate endDate, UUID organizationId) {
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
            // Không có hoạt động nào trong đủ 30 ngày gần nhất (kể cả mốc đúng 30 ngày) thì cần hỗ trợ
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

    /**
     * Xuất báo cáo mức độ sử dụng theo kỳ ra file CSV.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsv(LocalDate startDate, LocalDate endDate, UUID organizationId) {
        OrganizationUsageDashboardResponse dashboard = getDashboard(startDate, endDate, organizationId);
        StringBuilder csv = new StringBuilder();
        // BOM để Excel mở đúng tiếng Việt
        csv.append('\uFEFF');
        csv.append("Ma to chuc,Ten to chuc,Loai,Trang thai,Ngay tao,Ky hien tai,Ky truoc,")
                .append("Lo SX (hien tai),Lo SX (ky truoc),Lo SX (thay doi),Lo SX (% thay doi),")
                .append("Nhat ky (hien tai),Nhat ky (ky truoc),Nhat ky (thay doi),Nhat ky (% thay doi),")
                .append("Su kien (hien tai),Su kien (ky truoc),Su kien (thay doi),Su kien (% thay doi),")
                .append("Tem kich hoat (hien tai),Tem kich hoat (ky truoc),Tem kich hoat (thay doi),Tem kich hoat (% thay doi),")
                .append("Tra cuu (hien tai),Tra cuu (ky truoc),Tra cuu (thay doi),Tra cuu (% thay doi),")
                .append("Nguoi dung (hien tai),Nguoi dung (ky truoc),Nguoi dung (thay doi),Nguoi dung (% thay doi),")
                .append("Hoat dong gan nhat,Can ho tro,Trang thai du lieu\n");
        for (OrganizationUsageItem item : dashboard.getItems()) {
            csv.append(escape(item.getOrganizationCode())).append(',')
                    .append(escape(item.getOrganizationName())).append(',')
                    .append(escape(item.getOrganizationType())).append(',')
                    .append(escape(item.getOrganizationStatus())).append(',')
                    .append(formatDateTime(item.getCreatedAt())).append(',')
                    .append(dashboard.getStartDate().format(CSV_DATE)).append(" - ")
                    .append(dashboard.getEndDate().format(CSV_DATE)).append(',')
                    .append(dashboard.getPreviousStartDate().format(CSV_DATE)).append(" - ")
                    .append(dashboard.getPreviousEndDate().format(CSV_DATE)).append(',');
            appendMetric(csv, item.getProductionLots());
            appendMetric(csv, item.getFarmLogs());
            appendMetric(csv, item.getChainEvents());
            appendMetric(csv, item.getActivatedLabels());
            appendMetric(csv, item.getPublicLookups());
            appendMetric(csv, item.getActiveUsers());
            csv.append(formatDateTime(item.getLastActivityAt())).append(',')
                    .append(item.isNeedsSupport() ? "Co" : "Khong").append(',')
                    .append(item.isHasData() ? "Co du lieu" : "Chua co du lieu").append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Chuẩn hóa kỳ hiện tại/kỳ trước. Kỳ trước có độ dài tương đương kỳ hiện
     * tại và kết thúc vào ngày liền trước kỳ hiện tại.
     */
    private ResolvedPeriod resolvePeriod(LocalDate startDate, LocalDate endDate) {
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

    /**
     * Tải danh sách tổ chức (một tổ chức khi có lọc, toàn hệ thống khi không).
     */
    private List<Organization> loadOrganizations(UUID organizationId) {
        if (organizationId != null) {
            return organizationRepository.findById(organizationId)
                    .map(List::of)
                    .orElse(List.of());
        }
        return organizationRepository.findAll();
    }

    /**
     * So sánh một chỉ số giữa hai kỳ. Khi kỳ trước bằng 0 thì phần trăm
     * thay đổi là null (không trả Infinity/NaN).
     */
    MetricComparison compare(long current, long previous) {
        long change = current - previous;
        Double changePercent = previous == 0 ? null : (change * 100.0 / previous);
        return MetricComparison.builder()
                .current(current)
                .previous(previous)
                .change(change)
                .changePercent(changePercent)
                .build();
    }

    private boolean hasAnyActivity(MetricComparison... metrics) {
        for (MetricComparison metric : metrics) {
            if (metric.getCurrent() > 0 || metric.getPrevious() > 0) {
                return true;
            }
        }
        return false;
    }

    private Map<UUID, Long> toCountMap(List<Object[]> rows) {
        Map<UUID, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    private Map<UUID, LocalDateTime> toTimeMap(List<Object[]> rows) {
        Map<UUID, LocalDateTime> result = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null && row[1] != null) {
                result.put((UUID) row[0], (LocalDateTime) row[1]);
            }
        }
        return result;
    }

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

    private void appendMetric(StringBuilder csv, MetricComparison metric) {
        csv.append(metric.getCurrent()).append(',')
                .append(metric.getPrevious()).append(',')
                .append(metric.getChange()).append(',')
                .append(metric.getChangePercent() != null ? String.format("%.2f", metric.getChangePercent()) : "").append(',');
    }

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

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(CSV_DATETIME) : "";
    }

    /**
     * Kỳ hiện tại/kỳ trước đã chuẩn hóa kèm mốc thời gian truy vấn.
     */
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
