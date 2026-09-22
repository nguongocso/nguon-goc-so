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
 */
@Service
public class ScanAnomalyDetectionServiceImpl implements ScanAnomalyDetectionService {
    private static final int DETECTION_WINDOW_MINUTES = 10;
    private static final double SAME_LOCATION_THRESHOLD_KM = 5.0;
    private static final int MIN_SCAN_COUNT = 3;
    private static final int MIN_DISTINCT_LOCATIONS = 2;
    private static final int HIGH_SEVERITY_DISTINCT_LOCATIONS = 3;

    private final TraceCodeScanLogRepository traceCodeScanLogRepository;
    private final NotificationService notificationService;
    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;
    private final TraceCodeRepository traceCodeRepository;
    private final AnomalyThresholdService anomalyThresholdService;

    /** Khởi tạo ScanAnomalyDetectionServiceImpl với các dependency cơ bản. */
    public ScanAnomalyDetectionServiceImpl(
            TraceCodeScanLogRepository traceCodeScanLogRepository,
            NotificationService notificationService,
            AlertRepository alertRepository,
            ObjectMapper objectMapper,
            TraceCodeRepository traceCodeRepository) {
        this(traceCodeScanLogRepository, notificationService, alertRepository, objectMapper, traceCodeRepository, null);
    }

    /** Khởi tạo ScanAnomalyDetectionServiceImpl đầy đủ với AnomalyThresholdService. */
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

    /** Kiểm tra và xử lý khi phát sinh lượt quét mới đối với mã truy xuất. */
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
            if (ScanAnomalyUtils.isWithinGracePeriod(traceCode.getActivatedAt(), LocalDateTime.now(),
                    gracePeriodDays)) {
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

    /** Lấy cấu hình ngưỡng quét bất thường có hiệu lực áp dụng cho mã truy xuất. */
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

    /** Lấy danh sách các lượt quét gần nhất trong cửa sổ thời gian xác định. */
    private List<TraceCodeScanLog> getRecentScanLogs(UUID traceCodeId, int windowMinutes) {
        LocalDateTime fromTime = LocalDateTime.now().minusMinutes(windowMinutes);

        return traceCodeScanLogRepository
                .findByTraceCodeIdAndScannedAtGreaterThanEqualOrderByScannedAtDesc(
                        traceCodeId,
                        fromTime);
    }

    /** Đánh dấu các lượt quét trong cửa sổ phát hiện là bất thường và lưu lý do (QTN-10). */
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

    /** Kiểm tra xem hai lượt quét có thuộc cùng một vị trí địa lý hay không. */
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

    /** Đếm số lượng vị trí địa lý khác nhau từ danh sách các lượt quét. */
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

    /** Đánh giá xem các lượt quét có thỏa mãn điều kiện bất thường hay không. */
    private boolean isAnomaly(List<TraceCodeScanLog> scanLogs) {

        if (scanLogs.size() < MIN_SCAN_COUNT) {
            return false;
        }

        int distinctLocations = countDistinctLocations(scanLogs);

        return distinctLocations >= MIN_DISTINCT_LOCATIONS;
    }

    /** Tạo đối tượng chi tiết cảnh báo chứa danh sách điểm quét và số lần quét. */
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

    /** Chuyển đổi lượt quét sang đối tượng điểm quét ScanPoint. */
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

    /** Xác định mức độ nghiêm trọng của cảnh báo dựa trên số vị trí quét khác nhau. */
    private AlertSeverity calculateSeverity(
            List<TraceCodeScanLog> scanLogs) {

        int distinctLocations = countDistinctLocations(scanLogs);

        if (distinctLocations >= HIGH_SEVERITY_DISTINCT_LOCATIONS) {
            return AlertSeverity.HIGH;
        }

        return AlertSeverity.MEDIUM;
    }

    /** Lấy thông tin tổ chức sở hữu từ mã truy xuất. */
    private Organization getOrganizationFromTraceCode(TraceCode traceCode) {
        if (traceCode != null && traceCode.getShipment() != null) {
            return traceCode.getShipment().getOrganization();
        }
        return null;
    }

    /** Tạo mới và lưu bản ghi cảnh báo quét bất thường vào cơ sở dữ liệu. */
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

    /** Gửi thông báo cảnh báo quét bất thường đến người dùng liên quan. */
    private void sendNotification(Alert alert) {
        notificationService.sendScanAnomalyNotification(alert);
    }
}