package vn.nguongocso.event.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import vn.nguongocso.event.entity.ChainEvent;

/**
 * Service tập trung tính toán và kiểm chứng chuỗi băm liên kết
 * cho tính toàn vẹn dòng sự kiện (NCL-08-CN-006).
 *
 * <p>Quy tắc băm theo API docs:</p>
 * <pre>
 * hash = SHA-256(
 *     eventType +
 *     shipmentId +
 *     recordedAt +
 *     recordedBy +
 *     eventData (JSON canonical, sort keys) +
 *     previousHash
 * )
 * </pre>
 *
 * <p>previousHash của sự kiện đầu tiên = chuỗi rỗng "".
 * Kết quả hash là chuỗi hex 64 ký tự.</p>
 */
@Service
public class EventHashService {

    /** Định dạng thời gian dùng trong canonical hash (ISO-ish, không có zone). */
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** Tên thuật toán băm theo API contract. */
    public static final String HASH_ALGORITHM = "SHA-256";

    private final ObjectMapper objectMapper;

    public EventHashService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Tính hash cho một sự kiện mới dựa trên previousHash.
     */
    public String calculateHash(ChainEvent event, String previousHash) {
        StringBuilder canonical = new StringBuilder();

        // eventType
        canonical.append(event.getEventType() != null ? event.getEventType().name() : "");

        // shipmentId
        canonical.append(event.getShipment() != null && event.getShipment().getId() != null
                ? event.getShipment().getId().toString()
                : "");

        // recordedAt (canonical form)
        canonical.append(formatTime(event.getRecordedAt()));

        // recordedBy (user id)
        canonical.append(event.getRecordedBy() != null && event.getRecordedBy().getUserId() != null
                ? event.getRecordedBy().getUserId().toString()
                : "");

        // eventData (canonical JSON - sorted keys)
        canonical.append(canonicalizeEventData(event.getEventData()));

        // previousHash (empty for genesis event)
        canonical.append(previousHash != null ? previousHash : "");

        return sha256Hex(canonical.toString());
    }

    /**
     * Sort eventData JSON keys for deterministic hashing.
     * Parses the raw JSON string and re-serializes with sorted keys.
     */
    public String canonicalizeEventData(String eventDataJson) {
        if (!StringUtils.hasText(eventDataJson)) {
            return "";
        }
        try {
            Map<String, Object> map = objectMapper.readValue(eventDataJson,
                    new TypeReference<Map<String, Object>>() {});
            return objectMapper.writeValueAsString(new TreeMap<>(map));
        } catch (Exception e) {
            // If parsing fails, fall back to trimming whitespace to keep determinism.
            return eventDataJson.trim();
        }
    }

    /**
     * Sắp xếp sự kiện theo recordedAt tăng dần, với thứ tự phụ theo id để ổn định.
     */
    public Comparator<ChainEvent> eventOrdering() {
        return Comparator
                .comparing(ChainEvent::getRecordedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(e -> e.getId() != null ? e.getId().toString() : "");
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        // Lấy giây để đảm bảo nhất quán; loại bỏ nano để hash ổn định.
        LocalDateTime secondPrecision = time.withNano(0);
        return secondPrecision.format(TIME_FORMAT);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}