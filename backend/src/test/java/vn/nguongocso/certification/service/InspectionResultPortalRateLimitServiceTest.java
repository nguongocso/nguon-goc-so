package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import vn.nguongocso.exception.BusinessException;

/**
 * Kiểm thử đơn vị cho InspectionResultPortalRateLimitService (QTN-20, MAJOR 6).
 * Đảm bảo cơ chế rate limiting trả về HTTP 429 Too Many Requests khi vượt ngưỡng.
 */
class InspectionResultPortalRateLimitServiceTest {

    private InspectionResultPortalRateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new InspectionResultPortalRateLimitService();
    }

    @Test
    @DisplayName("Token rate limit: Cho phép tối đa 60 requests/giờ, request thứ 61 ném HTTP 429")
    void testCheckTokenRateLimit_Exceeded_Throws429() {
        String tokenHash = "dummy-token-hash-" + System.currentTimeMillis();

        // 60 lần đầu tiên thành công
        for (int i = 0; i < 60; i++) {
            assertThatCode(() -> rateLimitService.checkTokenRateLimit(tokenHash))
                    .doesNotThrowAnyException();
        }

        // Lần thứ 61 vượt ngưỡng, ném HTTP 429
        assertThatThrownBy(() -> rateLimitService.checkTokenRateLimit(tokenHash))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(be.getMessage()).contains("thao tác quá nhanh");
                });
    }

    @Test
    @DisplayName("IP rate limit: 30 lần thử invalid token từ cùng IP thì bị chặn với HTTP 429")
    void testCheckIpRateLimit_Exceeded_Throws429() {
        String ipAddress = "192.168.1.100";

        // Ghi nhận 29 lần thất bại
        for (int i = 0; i < 29; i++) {
            rateLimitService.recordInvalidTokenAttempt(ipAddress);
            assertThatCode(() -> rateLimitService.checkIpRateLimit(ipAddress))
                    .doesNotThrowAnyException();
        }

        // Lần thất bại thứ 30
        rateLimitService.recordInvalidTokenAttempt(ipAddress);

        // Kiểm tra IP tiếp theo bị chặn với HTTP 429
        assertThatThrownBy(() -> rateLimitService.checkIpRateLimit(ipAddress))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(be.getMessage()).contains("thao tác quá nhanh");
                });
    }

    @Test
    @DisplayName("Token null hoặc rỗng không gây lỗi rate limit")
    void testCheckTokenRateLimit_NullOrBlank_NoError() {
        assertThatCode(() -> rateLimitService.checkTokenRateLimit(null)).doesNotThrowAnyException();
        assertThatCode(() -> rateLimitService.checkTokenRateLimit("")).doesNotThrowAnyException();
        assertThatCode(() -> rateLimitService.checkIpRateLimit(null)).doesNotThrowAnyException();
        assertThatCode(() -> rateLimitService.checkIpRateLimit("   ")).doesNotThrowAnyException();
    }
}
