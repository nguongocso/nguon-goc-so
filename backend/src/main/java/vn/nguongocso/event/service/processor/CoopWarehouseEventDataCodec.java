package vn.nguongocso.event.service.processor;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.exception.BusinessException;

/** Đọc và tuần tự hóa dữ liệu JSON của sự kiện kho HTX. */
final class CoopWarehouseEventDataCodec {

    private CoopWarehouseEventDataCodec() {}

    static String toJson(ObjectMapper objectMapper, Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Lỗi chuyển đổi dữ liệu sự kiện sang chuỗi JSON.");
        }
    }

    static LocalDateTime extractEntryTime(ObjectMapper objectMapper, ChainEvent event) {
        if (event.getEventData() != null) {
            try {
                Map<String, Object> data = readEventData(objectMapper, event.getEventData());
                if (data.get("entryTime") != null) {
                    return LocalDateTime.parse(data.get("entryTime").toString());
                }
            } catch (Exception e) {
                return event.getRecordedAt();
            }
        }
        return event.getRecordedAt();
    }

    static String extractStringField(ObjectMapper objectMapper, ChainEvent event, String key) {
        if (event.getEventData() != null) {
            try {
                Object value = readEventData(objectMapper, event.getEventData()).get(key);
                return value != null ? value.toString() : null;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static Map<String, Object> readEventData(ObjectMapper objectMapper, String eventData)
            throws JsonProcessingException {
        return objectMapper.readValue(eventData, new TypeReference<Map<String, Object>>() {});
    }
}
