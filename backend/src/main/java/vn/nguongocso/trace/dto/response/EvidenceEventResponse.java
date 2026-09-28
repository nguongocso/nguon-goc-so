package vn.nguongocso.trace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Thông tin sự kiện bằng chứng sản lượng. */
@Getter
@Setter
@Builder
public class EvidenceEventResponse {
    private UUID eventId;

    private String eventType;

    private LocalDateTime recordedAt;

    private String recordedByName;

    private UUID shipmentId;

    private UUID productionLotId;

    private String productionLotName;

    private BigDecimal quantity;
}
