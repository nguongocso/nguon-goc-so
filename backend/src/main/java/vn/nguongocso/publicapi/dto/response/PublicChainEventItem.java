package vn.nguongocso.publicapi.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response thông tin sự kiện trên chuỗi công khai.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicChainEventItem {

    private String eventType;

    private Map<String, Object> eventData;

    private LocalDateTime recordedAt;

    private Double latitude;

    private Double longitude;
}
