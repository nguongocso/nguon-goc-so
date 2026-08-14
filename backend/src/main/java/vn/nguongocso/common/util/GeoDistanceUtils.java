package vn.nguongocso.common.util;

/**
 * Tiện ích tính khoảng cách địa lý theo công thức Haversine.
 *
 * <p>
 * Được dùng chung cho hai chức năng nghiệp vụ độc lập:
 * <ul>
 * <li>NCL-08-CN-001 — phát hiện quét bất thường (cùng vị trí / vị trí khác nhau).</li>
 * <li>NCL-08-CN-007 — chấm điểm nghi vấn (khoảng cách di chuyển không hợp lý).</li>
 * </ul>
 *
 * <p>
 * Công thức này được tách ra từ hai bản sao giống hệt nhau trước đây trong
 * {@code ScanAnomalyDetectionServiceImpl} và {@code SuspectDetectionServiceImpl},
 * giữ nguyên hành vi số học hiện tại (bán kính Trái Đất 6371.0 km).
 * </p>
 */
public final class GeoDistanceUtils {

    /** Bán kính Trái Đất (km). */
    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoDistanceUtils() {
        // Utility class — không khởi tạo.
    }

    /**
     * Tính khoảng cách Haversine giữa hai tọa độ (km).
     *
     * @param lat1 vĩ độ điểm 1
     * @param lon1 kinh độ điểm 1
     * @param lat2 vĩ độ điểm 2
     * @param lon2 kinh độ điểm 2
     * @return khoảng cách tính bằng km
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}