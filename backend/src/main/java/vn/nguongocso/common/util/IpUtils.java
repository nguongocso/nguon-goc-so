package vn.nguongocso.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class IpUtils {

    private static final Set<String> TRUSTED_PROXY_IPS = parseTrustedProxyIps();

    private IpUtils() {
        // Prevent instantiation
    }

    /**
     * Lấy địa chỉ IP thực của client từ request hiện tại.
     * Nếu không thể lấy được (ví dụ chạy trong background thread), trả về "127.0.0.1".
     *
     * @return địa chỉ IP client hoặc "127.0.0.1" nếu không xác định được
     */
    public static String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "127.0.0.1";
        }
        HttpServletRequest request = attributes.getRequest();
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (isTrustedProxy(request.getRemoteAddr()) && xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Lấy IP đầu tiên (IP thực của client) nếu có nhiều proxy
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static boolean isTrustedProxy(String remoteAddress) {
        return TRUSTED_PROXY_IPS.contains(remoteAddress);
    }

    private static Set<String> parseTrustedProxyIps() {
        String configured = System.getenv().getOrDefault(
            "TRUSTED_PROXY_IPS",
            "127.0.0.1,::1,0:0:0:0:0:0:0:1"
        );
        return Arrays.stream(configured.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }
}