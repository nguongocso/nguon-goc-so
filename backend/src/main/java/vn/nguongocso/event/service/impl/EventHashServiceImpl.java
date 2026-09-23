package vn.nguongocso.event.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.service.EventHashService;
import vn.nguongocso.exception.BusinessException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventHashServiceImpl implements EventHashService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ObjectMapper objectMapper;

    @Override
    public String calculateHash(ChainEvent event, String previousHash) {
        try {
            StringBuilder canonical = new StringBuilder();

            // Thứ tự trường là một phần của hợp đồng băm chuỗi sự kiện.
            canonical.append(event.getEventType() != null ? event.getEventType().name() : "");

            canonical.append(event.getShipment() != null && event.getShipment().getId() != null
                    ? event.getShipment().getId().toString()
                    : "");

            canonical.append(formatTime(event.getRecordedAt()));

            canonical.append(event.getRecordedBy() != null && event.getRecordedBy().getUserId() != null
                    ? event.getRecordedBy().getUserId().toString()
                    : "");

            canonical.append(canonicalizeEventData(event.getEventData()));

            canonical.append(previousHash != null ? previousHash : "");

            return sha256Hex(canonical.toString());
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("Lỗi tính hash cho sự kiện: {}", e.getMessage(), e);
            throw new BusinessException("Lỗi tính mã băm sự kiện: " + e.getMessage());
        }
    }

    @Override
    public String canonicalizeEventData(String eventDataJson) {
        if (!StringUtils.hasText(eventDataJson)) {
            return "";
        }
        try {
            Map<String, Object> map = objectMapper.readValue(eventDataJson,
                    new TypeReference<Map<String, Object>>() {});
            return objectMapper.writeValueAsString(new TreeMap<>(map));
        } catch (JsonProcessingException e) {
            log.warn("Không thể parse JSON để sắp xếp keys: {}", e.getMessage());
            return eventDataJson.trim();
        }
    }

    @Override
    public Comparator<ChainEvent> eventOrdering() {
        return Comparator
                .comparing(ChainEvent::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(e -> e.getId() != null ? e.getId().toString() : "");
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
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
