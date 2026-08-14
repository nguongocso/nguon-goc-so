package vn.nguongocso.alert.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.alert.entity.AlertDetails;
import vn.nguongocso.alert.entity.ScanPoint;
import vn.nguongocso.alert.enums.AlertSeverity;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.alert.service.ScanAnomalyDetectionService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/** Triển khai phát hiện quét bất thường. */
@Service
@RequiredArgsConstructor
public class ScanAnomalyDetectionServiceImpl
        implements ScanAnomalyDetectionService {
    private static final int DETECTION_WINDOW_MINUTES = 10; // Khoảng thời gian xét các lượt quét (phút)
    private static final double EARTH_RADIUS_KM = 6371.0; // Bán kính Trái Đất (km)
    private static final double SAME_LOCATION_THRESHOLD_KM = 5.0; // Ngưỡng coi là cùng một vị trí (km)
    private static final int MIN_SCAN_COUNT = 3; // Số lượt quét tối thiểu để đánh giá
    private static final int MIN_DISTINCT_LOCATIONS = 2; // Số vị trí khác nhau tối thiểu để xác định bất thường

    private final TraceCodeScanLogRepository traceCodeScanLogRepository;
    private final NotificationService notificationService;
    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;
    private final TraceCodeRepository traceCodeRepository;

    /** Kiểm tra và xử lý khi phát sinh lượt quét mới. */
    @Override
    public void onScanRecorded(UUID traceCodeId) {

        List<TraceCodeScanLog> scanLogs = getRecentScanLogs(traceCodeId);

        boolean anomaly = isAnomaly(scanLogs);

        if (!anomaly) {
            return;
        }

        boolean existed = alertRepository
                .existsByRelatedEntityIdAndTypeAndStatus(
                        traceCodeId,
                        AlertType.SCAN_ANOMALY,
                        AlertStatus.PENDING);

        if (existed) {
            return;
        }

        Organization organization = getOrganizationFromTraceCode(traceCodeId);

        Alert alert = createAlert(
                traceCodeId,
                scanLogs,
                organization);

        sendNotification(alert);
    }

    /** Lấy các lượt quét gần nhất. */
    private List<TraceCodeScanLog> getRecentScanLogs(UUID traceCodeId) {

        LocalDateTime fromTime = LocalDateTime.now()
                .minusMinutes(DETECTION_WINDOW_MINUTES);

        return traceCodeScanLogRepository
                .findByTraceCodeIdAndScannedAtGreaterThanEqualOrderByScannedAtDesc(
                        traceCodeId,
                        fromTime);
    }

    /** Tính khoảng cách giữa hai tọa độ (km). */
    private double calculateDistance(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
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

        double distance = calculateDistance(
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

        if (distinctLocations >= 3) {
            return AlertSeverity.HIGH;
        }

        return AlertSeverity.MEDIUM;
    }

    /** Lấy tổ chức từ trace code. */
    private Organization getOrganizationFromTraceCode(UUID traceCodeId) {
        TraceCode traceCode = traceCodeRepository.findById(traceCodeId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy mã truy xuất."));
        return traceCode.getShipment().getOrganization();
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