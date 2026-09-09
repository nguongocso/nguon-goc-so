package vn.nguongocso.alert.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import vn.nguongocso.common.util.GeoDistanceUtils;
import vn.nguongocso.report.entity.TraceCodeScanLog;

/**
 * Tiện ích dùng chung phục vụ phát hiện quét bất thường và ước lượng tác động (NCL-08-CN-014).
 * <p>
 * Đồng bộ hóa logic kiểm tra giữa luồng quét thực tế (live scan evaluation) và
 * luồng chạy thử nghiệm ước lượng tác động (dry-run impact estimation).
 * </p>
 */
public final class ScanAnomalyUtils {

    private ScanAnomalyUtils() {
        // Tiện ích static không khởi tạo instance
    }

    /**
     * Kiểm tra xem một thời điểm quét có nằm trong thời gian ân hạn (grace period) hay không.
     * <p>
     * Trong thời gian ân hạn (từ lúc kích hoạt đến trước {@code gracePeriodDays} ngày),
     * hệ thống bỏ qua việc đánh giá quét bất thường đối với mã tem.
     * Chỉ khi số ngày trôi qua kể từ thời điểm kích hoạt lớn hơn hoặc bằng {@code gracePeriodDays}
     * thì việc đánh giá mới bắt đầu.
     * </p>
     *
     * @param activatedAt thời điểm kích hoạt mã tem
     * @param eventTime thời điểm diễn ra sự kiện quét
     * @param gracePeriodDays số ngày ân hạn
     * @return {@code true} nếu còn trong thời gian ân hạn (cần bỏ qua đánh giá), ngược lại {@code false}
     */
    public static boolean isWithinGracePeriod(LocalDateTime activatedAt, LocalDateTime eventTime, Integer gracePeriodDays) {
        if (activatedAt == null || eventTime == null || gracePeriodDays == null || gracePeriodDays <= 0) {
            return false;
        }
        long daysSinceActivation = Duration.between(activatedAt, eventTime).toDays();
        return daysSinceActivation < gracePeriodDays;
    }

    /**
     * Kiểm tra vi phạm tần suất quét cao theo cửa sổ trượt (rolling window) chuẩn:
     * <ul>
     * <li>Tồn tại cửa sổ trượt 24 giờ bất kỳ có số lượt quét >= maxScansPerDay.</li>
     * <li>Hoặc tồn tại cửa sổ trượt 1 giờ bất kỳ có số lượt quét >= maxScansPerHour.</li>
     * </ul>
     *
     * @param sortedScans danh sách lượt quét đã sắp xếp tăng dần theo thời gian
     * @param maxPerHour số lượt quét tối đa cho phép trong 1 giờ
     * @param maxPerDay số lượt quét tối đa cho phép trong 24 giờ
     * @return {@code true} nếu vi phạm tần suất quét, ngược lại {@code false}
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
     * Kiểm tra vi phạm khoảng cách di chuyển bất hợp lý (impossible travel):
     * <p>
     * Hai lượt quét liên tiếp có tọa độ GPS hợp lệ cách nhau lớn hơn {@code maxDistanceKm}
     * trong khoảng thời gian nhỏ hơn hoặc bằng {@code minTimeMinutes}.
     * </p>
     *
     * @param sortedScans danh sách lượt quét đã sắp xếp tăng dần theo thời gian
     * @param maxDistanceKm khoảng cách tối đa cho phép (km)
     * @param minTimeMinutes khung thời gian tối thiểu xét di chuyển (phút)
     * @return {@code true} nếu phát hiện di chuyển bất hợp lý, ngược lại {@code false}
     */
    public static boolean isImpossibleTravel(List<TraceCodeScanLog> sortedScans, double maxDistanceKm, int minTimeMinutes) {
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
