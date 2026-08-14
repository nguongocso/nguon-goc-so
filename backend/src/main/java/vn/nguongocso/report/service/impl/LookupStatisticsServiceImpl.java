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

/**
 * Service thống kê tra cứu mã truy xuất.
 *
 * @author Triệu Văn Đại
 */
@Service
@RequiredArgsConstructor
public class LookupStatisticsServiceImpl implements LookupStatisticsService {
    private final TraceCodeScanLogRepository traceCodeScanLogRepository;

    /**
     * Lấy thống kê tra cứu mã truy xuất dựa trên các tiêu chí lọc.
     *
     * @param startDate       Ngày bắt đầu (có thể null)
     * @param endDate         Ngày kết thúc (có thể null)
     * @param productionLotId ID của lô sản xuất (có thể null)
     * @param shipmentId      ID của lô hàng (có thể null)
     * @param organizationId  ID của tổ chức (có thể null)
     * @param groupBy         Cách nhóm dữ liệu theo thời gian (DAY, WEEK, MONTH,
     *                        YEAR)
     * @param currentUser     Thông tin người dùng hiện tại
     * @return Thống kê tra cứu mã truy xuất
     */
    @Override
    @Transactional(readOnly = true)
    public LookupStatisticsResponse getStatistics(LocalDate startDate, LocalDate endDate, UUID productionLotId,
            UUID shipmentId, UUID organizationId, String groupBy, CustomUserDetails currentUser) {
        // 1. Phân quyền và cách ly dữ liệu
        UUID targetOrgId = validateAndGetOrganizationId(organizationId, currentUser);

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

        // 2. Lấy dữ liệu thống kê tổng hợp (Summary)
        long totalScans = traceCodeScanLogRepository.countScans(targetOrgId, productionLotId, shipmentId, startDateTime,
                endDateTime);
        long totalUniqueCodes = traceCodeScanLogRepository.countUniqueCodes(targetOrgId, productionLotId, shipmentId,
                startDateTime, endDateTime);
        long abnormalScansCount = traceCodeScanLogRepository.countAbnormalScans(targetOrgId, productionLotId,
                shipmentId, startDateTime, endDateTime);

        LookupStatisticsResponse.SummaryStats summary = LookupStatisticsResponse.SummaryStats.builder()
                .totalScans(totalScans)
                .totalUniqueCodes(totalUniqueCodes)
                .abnormalScansCount(abnormalScansCount)
                .build();

        // 3. Thống kê theo địa điểm (Location)
        List<Object[]> locationStatsRaw = traceCodeScanLogRepository.getStatsByLocation(targetOrgId, productionLotId,
                shipmentId, startDateTime, endDateTime);
        List<LookupStatisticsResponse.LocationScanStats> byLocation = locationStatsRaw.stream()
                .map(row -> new LookupStatisticsResponse.LocationScanStats((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        // 4. Thống kê theo Lô sản xuất (ProductionLot)
        List<Object[]> lotStatsRaw = traceCodeScanLogRepository.getStatsByProductionLot(targetOrgId, productionLotId,
                shipmentId, startDateTime, endDateTime);
        List<LookupStatisticsResponse.LotScanStats> byProductionLot = lotStatsRaw.stream()
                .map(row -> new LookupStatisticsResponse.LotScanStats((UUID) row[0], (String) row[1], (Long) row[2],
                        row[3] != null ? ((Number) row[3]).longValue() : 0L))
                .collect(Collectors.toList());

        // 5. Thống kê chuỗi thời gian (TimeSeries)
        List<LocalDateTime> scannedAtList = traceCodeScanLogRepository.getScannedAtList(targetOrgId, productionLotId,
                shipmentId, startDateTime, endDateTime);
        List<LookupStatisticsResponse.TimeSeriesData> timeSeries = groupScannedAt(scannedAtList, groupBy);

        return LookupStatisticsResponse.builder()
                .summary(summary)
                .byLocation(byLocation)
                .byProductionLot(byProductionLot)
                .timeSeries(timeSeries)
                .build();
    }

    /**
     * Lấy danh sách các lần quét bất thường dựa trên các tiêu chí lọc.
     *
     * @param startDate       Ngày bắt đầu (có thể null)
     * @param endDate         Ngày kết thúc (có thể null)
     * @param productionLotId ID của lô sản xuất (có thể null)
     * @param organizationId  ID của tổ chức (có thể null)
     * @param pageable        Thông tin phân trang
     * @param currentUser     Thông tin người dùng hiện tại
     * @return Danh sách các lần quét bất thường
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AbnormalScanResponse> getAbnormalScans(LocalDate startDate, LocalDate endDate, UUID productionLotId,
            UUID organizationId, Pageable pageable, CustomUserDetails currentUser) {
        UUID targetOrgId = validateAndGetOrganizationId(organizationId, currentUser);

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

        Page<TraceCodeScanLog> rawLogs = traceCodeScanLogRepository.findAbnormalScans(targetOrgId, productionLotId,
                startDateTime, endDateTime, pageable);

        return rawLogs.map(log -> AbnormalScanResponse.builder()
                .scanId(log.getId())
                .codeValue(log.getTraceCode().getCodeValue())
                .lotName(log.getTraceCode().getShipment().getProductionLot().getName())
                .scannedAt(log.getScannedAt())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .location(log.getLocation())
                .latitude(log.getLatitude() != null ? log.getLatitude().doubleValue() : null)
                .longitude(log.getLongitude() != null ? log.getLongitude().doubleValue() : null)
                .reason(log.getAbnormalReason())
                .build());
    }

    private UUID validateAndGetOrganizationId(UUID organizationId, CustomUserDetails currentUser) {
        String role = currentUser.getRoleCode();
        if ("VT-01".equals(role)) {
            return organizationId; // Admin có quyền lọc theo bất kỳ org nào
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

    private List<LookupStatisticsResponse.TimeSeriesData> groupScannedAt(List<LocalDateTime> list, String groupBy) {
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
