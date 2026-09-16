package vn.nguongocso.alert.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.dto.response.AnomalyThresholdResponse;
import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.alert.entity.AlertDetails;
import vn.nguongocso.alert.entity.ScanPoint;
import vn.nguongocso.alert.enums.AlertSeverity;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.alert.service.AnomalyThresholdService;
import vn.nguongocso.alert.service.ScanAnomalyDetectionService;
import vn.nguongocso.alert.util.ScanAnomalyUtils;
import vn.nguongocso.common.util.GeoDistanceUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Triển khai phát hiện quét bất thường (NCL-08-CN-001).
 *
 * <p>
 * Service này chỉ chịu trách nhiệm phát hiện quét bất thường, đánh dấu các lượt
 * quét bất thường (QTN-10), tạo cảnh báo {@code SCAN_ANOMALY}, tính mức độ (severity)
 * và gửi thông báo. Nó KHÔNG chấm điểm nghi vấn và KHÔNG phụ thuộc vào
 * {@code SuspectDetectionService} (NCL-08-CN-007).
 * </p>
 */
@Service
public class ScanAnomalyDetectionServiceImpl
        implements ScanAnomalyDetectionService {
    private static final int DETECTION_WINDOW_MINUTES = 10; // Khoảng thời gian xét các lượt quét mặc định (phút)
    private static final double SAME_LOCATION_THRESHOLD_KM = 5.0; // Ngưỡng coi là cùng một vị trí (km)
    private static final int MIN_SCAN_COUNT = 3; // Số lượt quét tối thiểu để đánh giá
    private static final int MIN_DISTINCT_LOCATIONS = 2; // Số vị trí khác nhau tối thiểu để xác định bất thường
    private static final int HIGH_SEVERITY_DISTINCT_LOCATIONS = 3; // Ngưỡng vị trí để xếp mức HIGH

    private final TraceCodeScanLogRepository traceCodeScanLogRepository;
    private final NotificationService notificationService;
    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;
    private final TraceCodeRepository traceCodeRepository;
    private final AnomalyThresholdService anomalyThresholdService;

    public ScanAnomalyDetectionServiceImpl(
            TraceCodeScanLogRepository traceCodeScanLogRepository,
            NotificationService notificationService,
            AlertRepository alertRepository,
            ObjectMapper objectMapper,
            TraceCodeRepository traceCodeRepository) {
        this(traceCodeScanLogRepository, notificationService, alertRepository, objectMapper, traceCodeRepository, null);
    }

    @Autowired
    public ScanAnomalyDetectionServiceImpl(
            TraceCodeScanLogRepository traceCodeScanLogRepository,
            NotificationService notificationService,
            AlertRepository alertRepository,
            ObjectMapper objectMapper,
            TraceCodeRepository traceCodeRepository,
            @Autowired(required = false) AnomalyThresholdService anomalyThresholdService) {
        this.traceCodeScanLogRepository = traceCodeScanLogRepository;
        this.notificationService = notificationService;
        this.alertRepository = alertRepository;
        this.objectMapper = objectMapper;
        this.traceCodeRepository = traceCodeRepository;
        this.anomalyThresholdService = anomalyThresholdService;
    }

    /** Kiểm tra và xử lý khi phát sinh lượt quét mới. */
    @Override
    @Transactional
    public void onScanRecorded(UUID traceCodeId) {
        TraceCode traceCode = traceCodeRepository.findById(traceCodeId).orElse(null);
        if (traceCode == null) {
            return;
        }

        AnomalyThresholdResponse threshold = getEffectiveThreshold(traceCode);

        // Gate: Grace period check (P1.1 / P1.4)
        if (traceCode.getActivatedAt() != null) {
            int gracePeriodDays = (threshold != null && threshold.getActivationAgeDays() != null)
                    ? threshold.getActivationAgeDays()
                    : AnomalyThresholdServiceImpl.DEFAULT_ACTIVATION_AGE_DAYS;
            if (ScanAnomalyUtils.isWithinGracePeriod(traceCode.getActivatedAt(), LocalDateTime.now(), gracePeriodDays)) {
                return;
            }
        }

        int windowMinutes = (threshold != null && threshold.getMinTimeBetweenScansMinutes() != null)
                ? threshold.getMinTimeBetweenScansMinutes()
                : DETECTION_WINDOW_MINUTES;

        List<TraceCodeScanLog> scanLogs = getRecentScanLogs(traceCodeId, windowMinutes);

        boolean anomaly = isAnomaly(scanLogs);

        if (!anomaly) {
            return;
        }

        // QTN-10: đánh dấu các lượt quét bất thường để báo cáo thống kê đọc được.
        markScanLogsAbnormal(scanLogs, windowMinutes);

        boolean existed = alertRepository
                .existsByRelatedEntityIdAndTypeAndStatus(
                        traceCodeId,
                        AlertType.SCAN_ANOMALY,
                        AlertStatus.PENDING);

        if (existed) {
            return;
        }

        Organization organization = getOrganizationFromTraceCode(traceCode);

        Alert alert = createAlert(
                traceCodeId,
                scanLogs,
                organization);

        sendNotification(alert);
    }

    private AnomalyThresholdResponse getEffectiveThreshold(TraceCode traceCode) {
        if (anomalyThresholdService == null || traceCode == null) {
            return null;
        }
        UUID categoryId = null;
        if (traceCode.getShipment() != null && traceCode.getShipment().getProductionLot() != null
                && traceCode.getShipment().getProductionLot().getProductCategory() != null) {
            categoryId = traceCode.getShipment().getProductionLot().getProductCategory().getId();
        }
        return anomalyThresholdService.getEffectiveThreshold(categoryId);
    }

    /** Lấy các lượt quét gần nhất. */
    private List<TraceCodeScanLog> getRecentScanLogs(UUID traceCodeId, int windowMinutes) {
        LocalDateTime fromTime = LocalDateTime.now().minusMinutes(windowMinutes);

        return traceCodeScanLogRepository
                .findByTraceCodeIdAndScannedAtGreaterThanEqualOrderByScannedAtDesc(
                        traceCodeId,
                        fromTime);
    }

    /**
     * Đánh dấu các lượt quét trong cửa sổ phát hiện là bất thường (QTN-10).
     *
     * <p>
     * Ghi {@code isAbnormal = true} và lý do bất thường vào các bản ghi hiện có,
     * không tạo cột/bảng mới.
     * </p>
     */
    private void markScanLogsAbnormal(List<TraceCodeScanLog> scanLogs, int windowMinutes) {
        int distinctLocations = countDistinctLocations(scanLogs);

        for (TraceCodeScanLog scanLog : scanLogs) {
            scanLog.setIsAbnormal(true);
            scanLog.setAbnormalReason("Phát hiện quét bất thường: "
                    + scanLogs.size() + " lượt quét tại "
                    + distinctLocations + " vị trí khác nhau trong "
                    + windowMinutes + " phút.");
        }

        traceCodeScanLogRepository.saveAll(scanLogs);
    }

    /** Kiểm tra hai vị trí có giống nhau. */
    private boolean isSameLocation(
            TraceCodeScanLog first,
            TraceCodeScanLog second) {

        if (first.getLatitude() == null
                || first.getLongitude() == null
                || second.getLatitude() == null
                || second.getLongitude() == null) {
            return false;
        }

        double distance = GeoDistanceUtils.haversineKm(
                first.getLatitude().doubleValue(),
                first.getLongitude().doubleValue(),
                second.getLatitude().doubleValue(),
                second.getLongitude().doubleValue());

        return distance <= SAME_LOCATION_THRESHOLD_KM;
    }

    private int countDistinctLocations(List<TraceCodeScanLog> scanLogs) {

        List<TraceCodeScanLog> distinctLocations = new ArrayList<>();

        for (TraceCodeScanLog scanLog : scanLogs) {

            // Không có tọa độ thì không thể xác định vị trí
            if (scanLog.getLatitude() == null
                    || scanLog.getLongitude() == null) {
                continue;
            }

            boolean existed = false;

            for (TraceCodeScanLog location : distinctLocations) {

                if (isSameLocation(scanLog, location)) {
                    existed = true;
                    break;
                }
            }

            if (!existed) {
                distinctLocations.add(scanLog);
            }
        }

        return distinctLocations.size();
    }

    /** Kiểm tra quét bất thường. */
    private boolean isAnomaly(List<TraceCodeScanLog> scanLogs) {

        if (scanLogs.size() < MIN_SCAN_COUNT) {
            return false;
        }

        int distinctLocations = countDistinctLocations(scanLogs);

        return distinctLocations >= MIN_DISTINCT_LOCATIONS;
    }

    /** Tạo chi tiết cảnh báo. */
    private AlertDetails buildAlertDetails(
            List<TraceCodeScanLog> scanLogs) {

        AlertDetails details = new AlertDetails();

        List<ScanPoint> locations = scanLogs.stream()
                .map(this::buildScanPoint)
                .toList();

        details.setLocations(locations);
        details.setScanCount(scanLogs.size());
        details.setThresholdConfigured(MIN_SCAN_COUNT);

        return details;
    }

    /** Tạo điểm quét. */
    private ScanPoint buildScanPoint(TraceCodeScanLog scanLog) {

        ScanPoint scanPoint = new ScanPoint();

        if (scanLog.getLatitude() != null) {
            scanPoint.setLatitude(scanLog.getLatitude().doubleValue());
        }

        if (scanLog.getLongitude() != null) {
            scanPoint.setLongitude(scanLog.getLongitude().doubleValue());
        }

        scanPoint.setScannedAt(scanLog.getScannedAt());

        return scanPoint;
    }

    /** Xác định mức độ cảnh báo. */
    private AlertSeverity calculateSeverity(
            List<TraceCodeScanLog> scanLogs) {

        int distinctLocations = countDistinctLocations(scanLogs);

        if (distinctLocations >= HIGH_SEVERITY_DISTINCT_LOCATIONS) {
            return AlertSeverity.HIGH;
        }

        return AlertSeverity.MEDIUM;
    }

    /** Lấy tổ chức từ trace code. */
    private Organization getOrganizationFromTraceCode(TraceCode traceCode) {
        if (traceCode != null && traceCode.getShipment() != null) {
            return traceCode.getShipment().getOrganization();
        }
        return null;
    }

    /** Tạo cảnh báo. */
    private Alert createAlert(
            UUID traceCodeId,
            List<TraceCodeScanLog> scanLogs,
            Organization organization) {

        Alert alert = new Alert();

        alert.setId(UUID.randomUUID());
        alert.setType(AlertType.SCAN_ANOMALY);

        alert.setRelatedEntityType("TRACE_CODE");
        alert.setRelatedEntityId(traceCodeId);

        alert.setSeverity(calculateSeverity(scanLogs));
        AlertDetails details = buildAlertDetails(scanLogs);

        try {
            alert.setDetails(
                    objectMapper.writeValueAsString(details));
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    "Không thể tạo dữ liệu cảnh báo.");
        }

        alert.setStatus(AlertStatus.PENDING);
        alert.setCreatedAt(LocalDateTime.now());
        alert.setOrganization(organization);
        alert.setMessage("Phát hiện quét bất thường đối với mã truy xuất. "
                + "Số lần quét: " + scanLogs.size()
                + ", số vị trí khác nhau: " + countDistinctLocations(scanLogs) + ".");

        return alertRepository.save(alert);
    }

    /** Gửi thông báo. */
    private void sendNotification(Alert alert) {
        notificationService.sendScanAnomalyNotification(alert);
    }
}