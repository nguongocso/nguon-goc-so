package vn.nguongocso.alert.service.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

/** Che dữ liệu nhạy cảm trước khi ghi giá trị trước/sau vào tệp export. */
@Component
@RequiredArgsConstructor
public class ActivityLogExportValueSanitizer {
    private static final String MASK = "***";
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "passwordhash", "token", "accesstoken", "refreshtoken", "idtoken",
            "secret", "clientsecret", "credential", "credentials", "apikey", "privatekey",
            "accesskey", "authorization", "cookie", "setcookie");
    private static final Pattern TEXT_SECRET = Pattern.compile(
            "(?i)(password|token|secret|credential|api[_-]?key|authorization)\\s*([:=])\\s*([^,;\\s]+)");

    private final ObjectMapper objectMapper;

    /** Trả về JSON đã che khóa nhạy cảm hoặc chuỗi đã che theo mẫu key-value phổ biến. */
    public String sanitize(String value) {
        if (value == null || value.isBlank()) return value;
        try {
            JsonNode root = objectMapper.readTree(value);
            maskRecursively(root);
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ignored) {
            Matcher matcher = TEXT_SECRET.matcher(value);
            return matcher.replaceAll("$1$2" + MASK);
        }
    }

    private void maskRecursively(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitive(field.getKey())) object.put(field.getKey(), MASK);
                else maskRecursively(field.getValue());
            }
        } else if (node.isArray()) {
            node.forEach(this::maskRecursively);
        }
    }

    private boolean isSensitive(String key) {
        String normalized = key == null ? "" : key.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        return SENSITIVE_KEYS.contains(normalized);
    }
}
