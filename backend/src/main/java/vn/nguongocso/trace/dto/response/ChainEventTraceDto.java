package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.event.enums.ChainEventType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainEventTraceDto {
    private UUID id;
    private ChainEventType eventType;
    private String eventTypeName;
    private LocalDateTime recordedAt;
    private String location;
    private boolean isCorrection;
}
