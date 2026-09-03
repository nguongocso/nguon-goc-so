package vn.nguongocso.unit.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import vn.nguongocso.common.util.IpUtils;

/**
 * Unit tests cho {@link IpUtils#resolveClientIp(String, String)}.
 *
 * <p>
 * Mặc định TRUSTED_PROXY_IPS bao gồm:
 * 127.0.0.1, ::1, 0:0:0:0:0:0:0:1, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
 * </p>
 */
@DisplayName("IpUtils - Client IP Resolution")
class IpUtilsTest {

    // =========================================================
    // Test 1: Direct IP (không qua proxy)
    // =========================================================

    @Nested
    @DisplayName("Direct IP - không có forwarding header")
    class DirectIpTests {

        @Test
        @DisplayName("remoteAddr = 9.9.9.9, không XFF → trả về 9.9.9.9")
        void directPublicIp_shouldReturnRemoteAddr() {
            String result = IpUtils.resolveClientIp("9.9.9.9", null);
            assertThat(result).isEqualTo("9.9.9.9");
        }

        @Test
        @DisplayName("remoteAddr = 9.9.9.9, XFF empty → trả về 9.9.9.9")
        void directPublicIp_withEmptyXff_shouldReturnRemoteAddr() {
            String result = IpUtils.resolveClientIp("9.9.9.9", "");
            assertThat(result).isEqualTo("9.9.9.9");
        }
    }

    // =========================================================
    // Test 2: Forwarded IP qua trusted proxy
    // =========================================================

    @Nested
    @DisplayName("Forwarded IP - qua trusted proxy")
    class ForwardedIpTests {

        @Test
        @DisplayName("remoteAddr = 10.42.0.1 (trusted), XFF = 9.9.9.9 → trả về 9.9.9.9")
        void trustedProxy_withSingleXff_shouldReturnClientIp() {
            String result = IpUtils.resolveClientIp("10.42.0.1", "9.9.9.9");
            assertThat(result).isEqualTo("9.9.9.9");
        }

        @Test
        @DisplayName("remoteAddr = 127.0.0.1 (trusted), XFF = 9.9.9.9 → trả về 9.9.9.9")
        void loopbackProxy_withXff_shouldReturnClientIp() {
            String result = IpUtils.resolveClientIp("127.0.0.1", "9.9.9.9");
            assertThat(result).isEqualTo("9.9.9.9");
        }

        @Test
        @DisplayName("remoteAddr = 172.18.0.5 (docker bridge, trusted), XFF = 1.2.3.4 → trả về 1.2.3.4")
        void dockerBridgeProxy_withXff_shouldReturnClientIp() {
            String result = IpUtils.resolveClientIp("172.18.0.5", "1.2.3.4");
            assertThat(result).isEqualTo("1.2.3.4");
        }

        @Test
        @DisplayName("remoteAddr = 192.168.1.1 (LAN, trusted), XFF = 8.8.8.8 → trả về 8.8.8.8")
        void lanProxy_withXff_shouldReturnClientIp() {
            String result = IpUtils.resolveClientIp("192.168.1.1", "8.8.8.8");
            assertThat(result).isEqualTo("8.8.8.8");
        }
    }

    // =========================================================
    // Test 3: Private IP không có forwarding header
    // =========================================================

    @Nested
    @DisplayName("Private IP - không có forwarding header")
    class PrivateIpTests {

        @Test
        @DisplayName("remoteAddr = 10.42.0.1, không XFF → trả về 10.42.0.1")
        void privateIp_noXff_shouldReturnRemoteAddr() {
            String result = IpUtils.resolveClientIp("10.42.0.1", null);
            assertThat(result).isEqualTo("10.42.0.1");
        }

        @Test
        @DisplayName("remoteAddr = 10.42.0.1, XFF blank → trả về 10.42.0.1")
        void privateIp_blankXff_shouldReturnRemoteAddr() {
            String result = IpUtils.resolveClientIp("10.42.0.1", "   ");
            assertThat(result).isEqualTo("10.42.0.1");
        }
    }

    // =========================================================
    // Test 4: Spoofed header từ nguồn không tin cậy
    // =========================================================

    @Nested
    @DisplayName("Spoofed header - chống giả mạo IP")
    class SpoofedHeaderTests {

        @Test
        @DisplayName("remoteAddr = 203.0.113.1 (public, KHÔNG trusted), XFF = 9.9.9.9 → bỏ qua XFF")
        void untrustedPeer_withSpoofedXff_shouldIgnoreXff() {
            // Client gửi XFF giả nhưng kết nối trực tiếp (không qua proxy tin cậy)
            String result = IpUtils.resolveClientIp("203.0.113.1", "9.9.9.9");
            assertThat(result).isEqualTo("203.0.113.1");
        }

        @Test
        @DisplayName("remoteAddr = 8.8.8.8 (public, KHÔNG trusted), XFF = 1.1.1.1 → bỏ qua XFF")
        void publicPeer_withSpoofedXff_shouldIgnoreXff() {
            String result = IpUtils.resolveClientIp("8.8.8.8", "1.1.1.1");
            assertThat(result).isEqualTo("8.8.8.8");
        }
    }

    // =========================================================
    // Test 5: Multiple forwarded IPs
    // =========================================================

    @Nested
    @DisplayName("Multiple forwarded IPs - duyệt phải sang trái")
    class MultipleForwardedIpTests {

        @Test
        @DisplayName("XFF = '9.9.9.9, 10.42.0.5' → bỏ 10.42.0.5 (trusted), trả về 9.9.9.9")
        void multipleIps_shouldWalkRightToLeft() {
            String result = IpUtils.resolveClientIp("10.42.0.1", "9.9.9.9, 10.42.0.5");
            assertThat(result).isEqualTo("9.9.9.9");
        }

        @Test
        @DisplayName("XFF = '1.2.3.4, 10.0.0.1, 10.0.0.2' → trả về 1.2.3.4")
        void multipleTrustedProxies_shouldReturnFirstUntrusted() {
            String result = IpUtils.resolveClientIp("10.42.0.1", "1.2.3.4, 10.0.0.1, 10.0.0.2");
            assertThat(result).isEqualTo("1.2.3.4");
        }

        @Test
        @DisplayName("XFF = '9.9.9.9, 203.0.113.50' → 203.0.113.50 không trusted, trả về 203.0.113.50")
        void multipleIps_rightmostUntrusted_shouldReturnRightmost() {
            String result = IpUtils.resolveClientIp("10.42.0.1", "9.9.9.9, 203.0.113.50");
            assertThat(result).isEqualTo("203.0.113.50");
        }

        @Test
        @DisplayName("XFF toàn trusted = '10.0.0.1, 10.0.0.2' → fallback IP ngoài cùng bên trái")
        void allTrustedIps_shouldFallbackToLeftmost() {
            String result = IpUtils.resolveClientIp("10.42.0.1", "10.0.0.1, 10.0.0.2");
            assertThat(result).isEqualTo("10.0.0.1");
        }
    }

    // =========================================================
    // Test 6: Trusted proxy detection
    // =========================================================

    @Nested
    @DisplayName("Trusted proxy detection")
    class TrustedProxyDetectionTests {

        @Test
        @DisplayName("127.0.0.1 là trusted")
        void loopbackIpv4_shouldBeTrusted() {
            assertThat(IpUtils.isTrustedProxy("127.0.0.1")).isTrue();
        }

        @Test
        @DisplayName("::1 là trusted")
        void loopbackIpv6_shouldBeTrusted() {
            assertThat(IpUtils.isTrustedProxy("::1")).isTrue();
        }

        @Test
        @DisplayName("10.42.0.1 là trusted (10.0.0.0/8)")
        void k3sPodIp_shouldBeTrusted() {
            assertThat(IpUtils.isTrustedProxy("10.42.0.1")).isTrue();
        }

        @Test
        @DisplayName("172.18.0.5 là trusted (172.16.0.0/12)")
        void dockerBridgeIp_shouldBeTrusted() {
            assertThat(IpUtils.isTrustedProxy("172.18.0.5")).isTrue();
        }

        @Test
        @DisplayName("192.168.1.100 là trusted (192.168.0.0/16)")
        void lanIp_shouldBeTrusted() {
            assertThat(IpUtils.isTrustedProxy("192.168.1.100")).isTrue();
        }

        @Test
        @DisplayName("9.9.9.9 KHÔNG trusted (public IP)")
        void publicIp_shouldNotBeTrusted() {
            assertThat(IpUtils.isTrustedProxy("9.9.9.9")).isFalse();
        }

        @Test
        @DisplayName("203.0.113.1 KHÔNG trusted (public IP)")
        void anotherPublicIp_shouldNotBeTrusted() {
            assertThat(IpUtils.isTrustedProxy("203.0.113.1")).isFalse();
        }

        @Test
        @DisplayName("null KHÔNG trusted")
        void nullIp_shouldNotBeTrusted() {
            assertThat(IpUtils.isTrustedProxy(null)).isFalse();
        }

        @Test
        @DisplayName("empty string KHÔNG trusted")
        void emptyIp_shouldNotBeTrusted() {
            assertThat(IpUtils.isTrustedProxy("")).isFalse();
        }
    }
}