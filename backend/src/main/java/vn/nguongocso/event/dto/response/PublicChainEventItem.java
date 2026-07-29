package vn.nguongocso.event.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public class PublicChainEventItem {
    private String eventType;
    private Map<String, Object> eventData;
    private LocalDateTime recordedAt;
}
