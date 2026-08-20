// PublicTraceResponse.java
package vn.nguongocso.publicapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response thông tin tra cứu công khai.
 */
@Getter
@Setter
@Builder
public class PublicTraceResponse {
    private String codeValue;

    private UUID productionLotId;

    private String productName;

    private String shipmentCode;

    private String shipmentStatus;

    private Boolean recalled;

    private String recallMessage;

    private Boolean locked;

    private String lockReason;

    private LocalDateTime lockedAt;

    private List<PublicChainEventItem> events;
}