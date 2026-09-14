package vn.nguongocso.alert_reclaim_history.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.alert.service.impl.ActivityLogExportValueSanitizer;

class ActivityLogExportValueSanitizerTest {

    private ActivityLogExportValueSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new ActivityLogExportValueSanitizer(new ObjectMapper());
    }

    @Test
    @DisplayName("Che mật khẩu và khóa nhạy cảm trong JSON lồng nhau nhiều cấp")
    void sanitize_nestedJsonObject_shouldMaskSensitiveKeysRecursively() {
        String input = """
                {
                    "user": {
                        "name": "Tran Van A",
                        "accountDetails": {
                            "password": "superSecretPassword",
                            "secret": "myClientSecret",
                            "accessToken": "eyJhbGciOi...",
                            "apiKey": "AIzaSy..."
                        },
                        "credentials": {
                            "pin": "1234"
                        },
                        "role": "VT-02"
                    }
                }
                """;

        String output = sanitizer.sanitize(input);

        assertThat(output).doesNotContain("superSecretPassword");
        assertThat(output).doesNotContain("myClientSecret");
        assertThat(output).doesNotContain("eyJhbGciOi...");
        assertThat(output).doesNotContain("AIzaSy...");
        assertThat(output).doesNotContain("1234");
        assertThat(output).contains("\"password\":\"***\"");
        assertThat(output).contains("\"secret\":\"***\"");
        assertThat(output).contains("\"accessToken\":\"***\"");
        assertThat(output).contains("\"apiKey\":\"***\"");
        assertThat(output).contains("\"credentials\":\"***\"");
        assertThat(output).contains("\"name\":\"Tran Van A\"");
        assertThat(output).contains("\"role\":\"VT-02\"");
    }

    @Test
    @DisplayName("Che khóa nhạy cảm trong mảng JSON")
    void sanitize_jsonArray_shouldMaskSensitiveKeysInsideArrayElements() {
        String input = """
                {
                    "items": [
                        { "token": "token-1", "name": "item-1" },
                        { "refreshToken": "token-2", "name": "item-2" }
                    ]
                }
                """;

        String output = sanitizer.sanitize(input);

        assertThat(output).doesNotContain("token-1");
        assertThat(output).doesNotContain("token-2");
        assertThat(output).contains("\"token\":\"***\"");
        assertThat(output).contains("\"refreshToken\":\"***\"");
        assertThat(output).contains("\"name\":\"item-1\"");
        assertThat(output).contains("\"name\":\"item-2\"");
    }

    @Test
    @DisplayName("Che chuỗi văn bản không phải JSON theo mẫu key-value nhạy cảm")
    void sanitize_plainTextSecrets_shouldMaskPatternMatches() {
        String input = "Cập nhật tài khoản: password=mySecret123; apiKey: key999; username=nongdan; authorization=Bearer xyz";

        String output = sanitizer.sanitize(input);

        assertThat(output).doesNotContain("mySecret123");
        assertThat(output).doesNotContain("key999");
        assertThat(output).doesNotContain("Bearer xyz");
        assertThat(output).contains("password=***");
        assertThat(output).contains("apiKey:***");
        assertThat(output).contains("authorization=***");
        assertThat(output).contains("username=nongdan");
    }

    @Test
    @DisplayName("Dữ liệu không nhạy cảm được giữ nguyên")
    void sanitize_nonSensitiveContent_shouldRemainUnchanged() {
        String json = "{\"status\":\"ACTIVE\",\"lotNumber\":\"LOT-2026\"}";
        assertThat(sanitizer.sanitize(json)).contains("\"status\":\"ACTIVE\"", "\"lotNumber\":\"LOT-2026\"");

        String plainText = "Cập nhật trạng thái lô hàng thành THU_HOI";
        assertThat(sanitizer.sanitize(plainText)).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Null và chuỗi rỗng trả về nguyên bản")
    void sanitize_nullAndBlank_shouldReturnSame() {
        assertThat(sanitizer.sanitize(null)).isNull();
        assertThat(sanitizer.sanitize("")).isEmpty();
        assertThat(sanitizer.sanitize("   ")).isEqualTo("   ");
    }
}
