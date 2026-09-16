package vn.nguongocso.trace.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import vn.nguongocso.trace.enums.ShipmentStatus;

@Data @Builder
public class SplitPreviewResponse {
    private UUID shipmentId; private String shipmentName; private ShipmentStatus status;
    private UUID productionLotId; private String productionLotName; private long declaredQuantity;
    private long assignableQuantity; private long nonInactiveQuantity; private CodeRange availableCodeRange;
    private boolean canSplit; private String blockReasonCode; private String blockMessage;
    @Data @Builder public static class CodeRange { private String fromCode; private String toCode; private long quantity; }
}
