package vn.nguongocso.alert.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import vn.nguongocso.common.util.GeoDistanceUtils;
import vn.nguongocso.report.entity.TraceCodeScanLog;

/**
 * Tiện ích dùng chung phục vụ phát hiện quét bất thường và ước lượng tác động
 * (NCL-08-CN-014).
 */
public final class ScanAnomalyUtils {

    private ScanAnomalyUtils() {
    }

    /**
     * Kiểm tra xem một thời điểm quét có nằm trong thời gian ân hạn (grace period)
     * hay không.
     */
    public static boolean isWithinGracePeriod(LocalDateTime activatedAt, LocalDateTime eventTime,
            Integer gracePeriodDays) {
        if (activatedAt == null || eventTime == null || gracePeriodDays == null || gracePeriodDays <= 0) {
            return false;
        }
        long daysSinceActivation = Duration.between(activatedAt, eventTime).toDays();
        return daysSinceActivation < gracePeriodDays;
    }

    /**
     * Kiểm tra vi phạm tần suất quét cao theo cửa sổ trượt (rolling window) chuẩn
     */
    public static boolean isHighFrequency(List<TraceCodeScanLog> sortedScans, int maxPerHour, int maxPerDay) {
        if (sortedScans == null || sortedScans.isEmpty()) {
            return false;
        }
        int n = sortedScans.size();

        // 1. Kiểm tra cửa sổ trượt 24 giờ (maxPerDay)
        for (int i = 0; i < n; i++) {
            LocalDateTime windowStart = sortedScans.get(i).getScannedAt();
            LocalDateTime windowEnd = windowStart.plusHours(24);
            int count24h = 0;
            for (int j = i; j < n; j++) {
                LocalDateTime t = sortedScans.get(j).getScannedAt();
                if (t.isAfter(windowEnd)) {
                    break;
                }
                if (!t.isBefore(windowStart)) {
                    count24h++;
                }
            }
            if (count24h >= maxPerDay) {
                return true;
            }
        }

        // 2. Kiểm tra cửa sổ trượt 1 giờ (maxPerHour)
        for (int i = 0; i < n; i++) {
            LocalDateTime windowStart = sortedScans.get(i).getScannedAt();
            LocalDateTime windowEnd = windowStart.plusHours(1);
            int count1h = 0;
            for (int j = i; j < n; j++) {
                LocalDateTime t = sortedScans.get(j).getScannedAt();
                if (t.isAfter(windowEnd)) {
                    break;
                }
                if (!t.isBefore(windowStart)) {
                    count1h++;
                }
            }
            if (count1h >= maxPerHour) {
                return true;
            }
        }

        return false;
    }

    /**
     * Kiểm tra vi phạm khoảng cách di chuyển bất hợp lý (impossible travel)
     */
    public static boolean isImpossibleTravel(List<TraceCodeScanLog> sortedScans, double maxDistanceKm,
            int minTimeMinutes) {
        if (sortedScans == null || sortedScans.size() < 2) {
            return false;
        }
        for (int i = 0; i < sortedScans.size() - 1; i++) {
            TraceCodeScanLog prev = sortedScans.get(i);
            TraceCodeScanLog curr = sortedScans.get(i + 1);

            if (prev.getLatitude() != null && prev.getLongitude() != null
                    && curr.getLatitude() != null && curr.getLongitude() != null) {
                double distance = GeoDistanceUtils.haversineKm(
                        prev.getLatitude().doubleValue(), prev.getLongitude().doubleValue(),
                        curr.getLatitude().doubleValue(), curr.getLongitude().doubleValue());
                long minutes = Math.abs(Duration.between(prev.getScannedAt(), curr.getScannedAt()).toMinutes());
                if (distance > maxDistanceKm && minutes <= minTimeMinutes) {
                    return true;
                }
            }
        }
        return false;
    }
}
