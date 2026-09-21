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
import com.fasterxml.jackson.databind.node.ArrayNode;
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
            "accesskey", "authorization", "cookie", "setcookie", "jwt");
    private static final Pattern TEXT_SECRET = Pattern.compile(
            "(?i)([A-Za-z0-9_.-]*(?:password|token|secret|credential|api[_-]?key|"
                    + "private[_-]?key|access[_-]?key|authorization|cookie)[A-Za-z0-9_.-]*)"
                    + "\\s*([:=])\\s*([^,;\\r\\n]+)");

    private final ObjectMapper objectMapper;

    /** Trả về JSON đã che khóa nhạy cảm hoặc chuỗi đã che theo mẫu key-value phổ biến. */
    public String sanitize(String value) {
        if (value == null || value.isBlank())
            return value;
        try {
            JsonNode root = objectMapper.readTree(value);
            if (root != null && root.isTextual()) {
                return objectMapper.writeValueAsString(maskText(root.textValue()));
            }
            maskRecursively(root);
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ignored) {
            return maskText(value);
        }
    }

    /**
     * Đệ quy che dữ liệu nhạy cảm (NCL-08-CN-016).
     */
    private void maskRecursively(JsonNode node) {
        if (node == null)
            return;
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitive(field.getKey()))
                    object.put(field.getKey(), MASK);
                else if (field.getValue().isTextual()) {
                    object.put(field.getKey(), maskText(field.getValue().textValue()));
                } else
                    maskRecursively(field.getValue());
            }
        } else if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            for (int i = 0; i < array.size(); i++) {
                JsonNode item = array.get(i);
                if (item.isTextual())
                    array.set(i, objectMapper.getNodeFactory().textNode(maskText(item.textValue())));
                else
                    maskRecursively(item);
            }
        }
    }

    /**
     * Kiểm tra xem key có nhạy cảm không (NCL-08-CN-016).
     */
    private boolean isSensitive(String key) {
        String normalized = key == null ? "" : key.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        return SENSITIVE_KEYS.stream().anyMatch(normalized::endsWith);
    }

    /**
     * Che dữ liệu nhạy cảm theo mẫu key-value phổ biến (NCL-08-CN-016).
     */
    private String maskText(String value) {
        Matcher matcher = TEXT_SECRET.matcher(value);
        return matcher.replaceAll("$1$2" + MASK);
    }
}
