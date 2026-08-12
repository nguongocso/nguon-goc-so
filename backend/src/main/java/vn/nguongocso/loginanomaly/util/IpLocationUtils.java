package vn.nguongocso.loginanomaly.util;

/**
 * Suy ra vị trí từ địa chỉ IP.
 *
 * <p>
 * Hiện tại chỉ phân biệt các dải IP nội bộ; với IP công cộng trả về
 * "Không xác định". Có thể bổ sung tra cứu GeoIP ở đây sau.
 * </p>
 */
public final class IpLocationUtils {

    private IpLocationUtils() {
        // Prevent instantiation
    }

    /** Vị trí suy ra từ địa chỉ IP. */
    public static String infer(String ip) {
        if (ip == null || ip.isBlank()) {
            return "Không xác định";
        }
        if ("localhost".equalsIgnoreCase(ip) || ip.startsWith("127.")) {
            return "Máy nội bộ";
        }
        if (isPrivateOrCgnat(ip)) {
            return "Mạng nội bộ/VPN";
        }
        return "Không xác định";
    }

    /** Kiểm tra IP thuộc dải riêng tư (RFC 1918) hoặc CGNAT (RFC 6598). */
    private static boolean isPrivateOrCgnat(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);

            if (first == 10) {
                return true;
            }
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }
            if (first == 192 && second == 168) {
                return true;
            }
            // 100.64.0.0/10 (CGNAT)
            return first == 100 && second >= 64 && second <= 127;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
