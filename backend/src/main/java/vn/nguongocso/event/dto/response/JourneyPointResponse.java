package vn.nguongocso.event.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class JourneyPointResponse {

    private UUID eventId;
    private String eventType;
    private String eventName;
    private Double latitude;
    private Double longitude;
    private LocalDateTime recordedAt;
    private String description;
    private Integer order;
}
