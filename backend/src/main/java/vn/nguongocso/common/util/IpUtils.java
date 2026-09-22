package vn.nguongocso.common.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/** Tiện ích xác định địa chỉ IP thực của client từ HTTP request (hỗ trợ Trusted Proxy). */
public class IpUtils {
    private static final Logger log = LoggerFactory.getLogger(IpUtils.class);

    private static final List<TrustedProxy> TRUSTED_PROXIES = parseTrustedProxies();

    private IpUtils() {
    }

    /** Lấy địa chỉ IP thực của client từ HTTP request hiện tại. */
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

    /** Xác định địa chỉ IP client từ remoteAddr và header X-Forwarded-For. */
    public static String resolveClientIp(String remoteAddr, String xForwardedFor) {
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

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
                return ip;
            }
        }

        String leftmost = parts[0].trim();
        log.debug("Tất cả IP trong X-Forwarded-For đều là proxy tin cậy, dùng IP ngoài cùng bên trái: {}", leftmost);
        return leftmost;
    }

    /** Kiểm tra một địa chỉ IP có nằm trong danh sách proxy tin cậy hay không. */
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
                "127.0.0.1,::1,0:0:0:0:0:0:0:1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16"
        );
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(TrustedProxy::parse)
                .collect(Collectors.toUnmodifiableList());
    }

    /** Đại diện cho một proxy tin cậy theo IP đơn hoặc dải CIDR. */
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
