package vn.nguongocso.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.report.dto.response.AbnormalScanResponse;
import vn.nguongocso.report.dto.response.LookupStatisticsResponse;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.report.service.LookupStatisticsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/** Triển khai dịch vụ thống kê tra cứu mã truy xuất. */
@Service
@RequiredArgsConstructor
public class LookupStatisticsServiceImpl implements LookupStatisticsService {
    private final TraceCodeScanLogRepository traceCodeScanLogRepository;

    /** Lấy thống kê tra cứu mã truy xuất dựa trên các tiêu chí lọc. */
    @Override
    @Transactional(readOnly = true)
    public LookupStatisticsResponse getStatistics(
        LocalDate startDate,
        LocalDate endDate,
        UUID productionLotId,
        UUID shipmentId,
        UUID organizationId,
        String groupBy,
        CustomUserDetails currentUser) {

        UUID targetOrgId = validateAndGetOrganizationId(organizationId, currentUser);

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

        long totalScans = traceCodeScanLogRepository.countScans(
            targetOrgId,
            productionLotId,
            shipmentId,
            startDateTime,
            endDateTime);
        long totalUniqueCodes = traceCodeScanLogRepository.countUniqueCodes(
            targetOrgId,
            productionLotId,
            shipmentId,
            startDateTime,
            endDateTime);
        long abnormalScansCount = traceCodeScanLogRepository.countAbnormalScans(
            targetOrgId,
            productionLotId,
            shipmentId,
            startDateTime,
            endDateTime);

        LookupStatisticsResponse.SummaryStats summary = LookupStatisticsResponse.SummaryStats.builder()
            .totalScans(totalScans)
            .totalUniqueCodes(totalUniqueCodes)
            .abnormalScansCount(abnormalScansCount)
            .build();

        List<Object[]> locationStatsRaw = traceCodeScanLogRepository.getStatsByLocation(
            targetOrgId,
            productionLotId,
            shipmentId,
            startDateTime,
            endDateTime);
        List<LookupStatisticsResponse.LocationScanStats> byLocation = locationStatsRaw.stream()
            .map(row -> new LookupStatisticsResponse.LocationScanStats((String) row[0], (Long) row[1]))
            .collect(Collectors.toList());

        List<Object[]> lotStatsRaw = traceCodeScanLogRepository.getStatsByProductionLot(
            targetOrgId,
            productionLotId,
            shipmentId,
            startDateTime,
            endDateTime);
        List<LookupStatisticsResponse.LotScanStats> byProductionLot = lotStatsRaw.stream()
            .map(row -> new LookupStatisticsResponse.LotScanStats((UUID) row[0], (String) row[1], (Long) row[2],
                row[3] != null ? ((Number) row[3]).longValue() : 0L))
            .collect(Collectors.toList());

        List<LocalDateTime> scannedAtList = traceCodeScanLogRepository.getScannedAtList(
            targetOrgId,
            productionLotId,
            shipmentId,
            startDateTime,
            endDateTime);
        List<LookupStatisticsResponse.TimeSeriesData> timeSeries = groupScannedAt(scannedAtList, groupBy);

        return LookupStatisticsResponse.builder()
            .summary(summary)
            .byLocation(byLocation)
            .byProductionLot(byProductionLot)
            .timeSeries(timeSeries)
            .build();
    }

    /** Lấy danh sách các lần quét bất thường dựa trên các tiêu chí lọc. */
    @Override
    @Transactional(readOnly = true)
    public Page<AbnormalScanResponse> getAbnormalScans(
        LocalDate startDate,
        LocalDate endDate,
        UUID productionLotId,
        UUID organizationId,
        Pageable pageable,
        CustomUserDetails currentUser) {

        UUID targetOrgId = validateAndGetOrganizationId(organizationId, currentUser);

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

        Page<TraceCodeScanLog> rawLogs = traceCodeScanLogRepository.findAbnormalScans(
            targetOrgId,
            productionLotId,
            startDateTime,
            endDateTime,
            pageable);

        return rawLogs.map(log -> {
            String codeVal = log.getTraceCode() != null ? log.getTraceCode().getCodeValue() : "";
            String lotName = (log.getTraceCode() != null && log.getTraceCode().getShipment() != null && log.getTraceCode().getShipment().getProductionLot() != null)
                ? log.getTraceCode().getShipment().getProductionLot().getName()
                : "";

            return AbnormalScanResponse.builder()
                .scanId(log.getId())
                .codeValue(codeVal)
                .lotName(lotName)
                .scannedAt(log.getScannedAt())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .location(log.getLocation())
                .latitude(log.getLatitude() != null ? log.getLatitude().doubleValue() : null)
                .longitude(log.getLongitude() != null ? log.getLongitude().doubleValue() : null)
                .reason(log.getAbnormalReason())
                .build();
        });
    }

    /** Kiểm tra quyền và lấy ID tổ chức hợp lệ. */
    private UUID validateAndGetOrganizationId(
        UUID organizationId,
        CustomUserDetails currentUser) {

        String role = currentUser.getRoleCode();
        if ("VT-01".equals(role)) {
            return organizationId;
        } else if ("VT-02".equals(role)) {
            UUID userOrgId = currentUser.getOrganizationId();
            if (organizationId != null && !organizationId.equals(userOrgId)) {
                throw new BusinessException("Từ chối truy cập: Bạn không có quyền truy cập dữ liệu của tổ chức khác.");
            }
            return userOrgId;
        } else {
            throw new BusinessException("Từ chối thao tác: Bạn không có quyền xem báo cáo thống kê.");
        }
    }

    /** Nhóm danh sách thời điểm quét theo khoảng thời gian. */
    private List<LookupStatisticsResponse.TimeSeriesData> groupScannedAt(
        List<LocalDateTime> list,
        String groupBy) {

        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        String type = (groupBy == null) ? "MONTH" : groupBy.toUpperCase();
        Map<String, Long> groups = new LinkedHashMap<>();

        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");

        for (LocalDateTime time : list) {
            String key;
            switch (type) {
            case "DAY":
                key = time.format(dayFormatter);
                break;
            case "WEEK":
                int week = time.get(WeekFields.of(Locale.getDefault()).weekOfYear());
                key = String.format("%d-W%02d", time.getYear(), week);
                break;
            case "YEAR":
                key = time.format(yearFormatter);
                break;
            case "MONTH":
            default:
                key = time.format(monthFormatter);
                break;
            }
            groups.put(key, groups.getOrDefault(key, 0L) + 1);
        }

        return groups.entrySet().stream()
            .map(e -> new LookupStatisticsResponse.TimeSeriesData(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    }
}
