package vn.nguongocso.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tiện ích xác định IP thực của client từ HTTP request.
 *
 * <p>
 * Cơ chế trusted-proxy:
 * </p>
 * <ul>
 *   <li>Nếu peer trực tiếp ({@code remoteAddr}) KHÔNG nằm trong danh sách
 *       proxy tin cậy, mọi header {@code X-Forwarded-For} đều bị bỏ qua
 *       (chống spoofing) và trả về {@code remoteAddr}.</li>
 *   <li>Nếu peer trực tiếp là proxy tin cậy, duyệt chuỗi XFF từ
 *       <b>phải sang trái</b>, bỏ qua các IP cũng là proxy tin cậy,
 *       và trả về IP không tin cậy đầu tiên — đó chính là IP client thực.</li>
 * </ul>
 *
 * <p>
 * Danh sách proxy tin cậy được đọc từ biến môi trường
 * {@code TRUSTED_PROXY_IPS} (phân tách bằng dấu phẩy), hỗ trợ cả
 * IP đơn và CIDR. Mặc định bao gồm loopback và các dải mạng nội bộ
 * (Docker bridge, Kubernetes pod network).
 * </p>
 */
public class IpUtils {

    private static final Logger log = LoggerFactory.getLogger(IpUtils.class);

    /**
     * Danh sách proxy tin cậy, đọc một lần khi class được load.
     * Mặc định: loopback + các dải private (Docker / k3s / k8s pod network).
     */
    private static final List<TrustedProxy> TRUSTED_PROXIES = parseTrustedProxies();

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
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "127.0.0.1";
        }
        HttpServletRequest request = attributes.getRequest();
        String remoteAddr = request.getRemoteAddr();
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String resolvedIp = resolveClientIp(remoteAddr, xForwardedFor);
        log.debug("Client IP resolution: remoteAddr={}, xForwardedFor={}, resolvedClientIp={}",
                remoteAddr, xForwardedFor, resolvedIp);
        return resolvedIp;
    }

    /**
     * Logic xác định IP client, tách riêng để unit-test được.
     *
     * @param remoteAddr    IP của peer kết nối trực tiếp tới backend
     * @param xForwardedFor giá trị header X-Forwarded-For (có thể null)
     * @return IP client thực
     */
    public static String resolveClientIp(String remoteAddr, String xForwardedFor) {
        // Peer trực tiếp KHÔNG phải proxy tin cậy → remoteAddr chính là client.
        // Bỏ qua XFF để chống spoofing (client tự gửi header giả).
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        // Peer trực tiếp là proxy tin cậy. Duyệt XFF từ PHẢI sang TRÁI.
        // Mỗi proxy append IP của hop trước vào bên phải XFF,
        // nên IP ngoài cùng bên phải gần backend nhất.
        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            return remoteAddr;
        }

        String[] parts = xForwardedFor.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String ip = parts[i].trim();
            if (ip.isEmpty()) {
                continue;
            }
            if (!isTrustedProxy(ip)) {
                // IP không tin cậy đầu tiên từ phải sang = client thực.
                return ip;
            }
        }

        // Toàn bộ XFF đều là proxy tin cậy (hiếm gặp).
        // Fallback: trả về IP ngoài cùng bên trái.
        String leftmost = parts[0].trim();
        log.debug("Tất cả IP trong X-Forwarded-For đều là proxy tin cậy, dùng IP ngoài cùng bên trái: {}", leftmost);
        return leftmost;
    }

    /**
     * Kiểm tra một IP có nằm trong danh sách proxy tin cậy không.
     */
    public static boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        for (TrustedProxy proxy : TRUSTED_PROXIES) {
            if (proxy.matches(ip)) {
                return true;
            }
        }
        return false;
    }

    private static List<TrustedProxy> parseTrustedProxies() {
        String configured = System.getenv().getOrDefault(
                "TRUSTED_PROXY_IPS",
                // Mặc định: loopback + các dải private phổ biến
                // (Docker bridge 172.16-31.x, k3s/k8s pod 10.x, LAN 192.168.x)
                "127.0.0.1,::1,0:0:0:0:0:0:0:1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16"
        );
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(TrustedProxy::parse)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Đại diện cho một proxy tin cậy: IP đơn hoặc CIDR range.
     */
    static class TrustedProxy {
        private final byte[] networkBytes;
        private final int prefixLength;
        private final String exactIp;

        static TrustedProxy parse(String value) {
            if (value.contains("/")) {
                String[] parts = value.split("/", 2);
                try {
                    byte[] addr = InetAddress.getByName(parts[0]).getAddress();
                    int prefix = Integer.parseInt(parts[1]);
                    return new TrustedProxy(addr, prefix, null);
                } catch (UnknownHostException | NumberFormatException e) {
                    log.warn("CIDR không hợp lệ trong TRUSTED_PROXY_IPS: {}, dùng như IP đơn", value);
                    return new TrustedProxy(null, -1, value);
                }
            }
            return new TrustedProxy(null, -1, value);
        }

        TrustedProxy(byte[] networkBytes, int prefixLength, String exactIp) {
            this.networkBytes = networkBytes;
            this.prefixLength = prefixLength;
            this.exactIp = exactIp;
        }

        boolean matches(String ip) {
            if (exactIp != null) {
                return exactIp.equals(ip);
            }
            try {
                byte[] addrBytes = InetAddress.getByName(ip).getAddress();
                if (addrBytes.length != networkBytes.length) {
                    return false;
                }
                int fullBytes = prefixLength / 8;
                int remainingBits = prefixLength % 8;
                for (int i = 0; i < fullBytes && i < addrBytes.length; i++) {
                    if (addrBytes[i] != networkBytes[i]) {
                        return false;
                    }
                }
                if (remainingBits > 0 && fullBytes < addrBytes.length) {
                    int mask = 0xFF << (8 - remainingBits);
                    return (addrBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
                }
                return true;
            } catch (UnknownHostException e) {
                return false;
            }
        }
    }
}