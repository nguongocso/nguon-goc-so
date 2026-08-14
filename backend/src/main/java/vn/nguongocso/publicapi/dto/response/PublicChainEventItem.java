package vn.nguongocso.publicapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response thông tin sự kiện trên chuỗi công khai.
 */
@Getter
@Setter
@Builder
public class PublicChainEventItem {
    private String eventType;

    private Map<String, Object> eventData;

    private LocalDateTime recordedAt;

    private Double latitude;

    private Double longitude;
}