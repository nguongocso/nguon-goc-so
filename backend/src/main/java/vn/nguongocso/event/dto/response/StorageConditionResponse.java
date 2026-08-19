package vn.nguongocso.event.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.event.enums.ChainEventType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi cho sự kiện theo dõi điều kiện bảo quản.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
public class StorageConditionResponse {
    private UUID id;
    private ChainEventType eventType;
    private UUID shipmentId;
    private String shipmentName;
    private Double temperature;
    private Double humidity;
    private ThresholdInfo thresholds;
    private Boolean isTemperatureExceeded;
    private Boolean isHumidityExceeded;
    private String alertLevel; // OK, WARNING, CRITICAL
    private LocalDateTime recordedAt;
    private String recordedBy;
}