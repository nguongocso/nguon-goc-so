package vn.nguongocso.event.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.event.enums.ChainEventType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO phản hồi chuỗi sự kiện cung ứng.
 *
 * @author Team WEB 1
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
public class ChainEventResponse {
    private UUID id;

    private UUID shipmentId;

    private ChainEventType eventType;

    private Map<String, Object> eventData;

    private Double latitude;

    private Double longitude;

    private LocalDateTime recordedAt;

    private String recordedByName;

    private LocalDateTime createdAt;
}
