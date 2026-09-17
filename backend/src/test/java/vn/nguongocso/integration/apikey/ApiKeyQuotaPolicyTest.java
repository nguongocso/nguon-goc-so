package vn.nguongocso.integration.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import vn.nguongocso.integration.apikey.service.ApiKeyQuotaPolicy;

/**
 * Kiểm thử quy tắc ngưỡng cảnh báo hạn mức khóa truy cập (NCL-12-CN-005).
 * <p>
 * Ngưỡng dùng chung cho cảnh báo realtime, job đối soát và trang cảnh báo tổng hợp.
 */
class ApiKeyQuotaPolicyTest {

    private ApiKeyQuotaPolicy apiKeyQuotaPolicy;

    @BeforeEach
    void setUp() {
        apiKeyQuotaPolicy = new ApiKeyQuotaPolicy();
        ReflectionTestUtils.setField(apiKeyQuotaPolicy, "quotaWarningRatio", 0.8);
    }

    @Test
    @DisplayName("Ngưỡng = ceil(hạn mức × tỷ lệ): 10 -> 8, 100 -> 80, 21 -> 17")
    void warningThreshold_roundsUp() {
        assertEquals(8, apiKeyQuotaPolicy.warningThreshold(10));
        assertEquals(80, apiKeyQuotaPolicy.warningThreshold(100));
        assertEquals(4, apiKeyQuotaPolicy.warningThreshold(5));
        assertEquals(1, apiKeyQuotaPolicy.warningThreshold(1));
        assertEquals(17, apiKeyQuotaPolicy.warningThreshold(21));
    }

    @Test
    @DisplayName("Hạn mức không hợp lệ thì không có ngưỡng cảnh báo")
    void warningThreshold_invalidRateLimit() {
        assertEquals(0, apiKeyQuotaPolicy.warningThreshold(0));
        assertEquals(0, apiKeyQuotaPolicy.warningThreshold(-5));
    }

    @Test
    @DisplayName("Chạm ngưỡng theo tổng lượt trong ngày: đạt đúng hoặc vượt đều tính là chạm")
    void isReached_boundaries() {
        assertFalse(apiKeyQuotaPolicy.isReached(7, 10));
        assertTrue(apiKeyQuotaPolicy.isReached(8, 10));
        assertTrue(apiKeyQuotaPolicy.isReached(25, 10));
    }

    @Test
    @DisplayName("Hạn mức không hợp lệ thì không bao giờ chạm ngưỡng")
    void isReached_invalidRateLimit() {
        assertFalse(apiKeyQuotaPolicy.isReached(5, 0));
    }

    @Test
    @DisplayName("Tỷ lệ ngưỡng hiển thị dạng phần trăm")
    void warningThresholdPercent_returnsPercent() {
        assertEquals(80, apiKeyQuotaPolicy.warningThresholdPercent());
    }
}