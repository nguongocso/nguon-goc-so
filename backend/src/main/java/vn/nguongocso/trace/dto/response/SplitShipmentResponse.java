package vn.nguongocso.trace.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import vn.nguongocso.trace.enums.ShipmentStatus;

@Data @Builder
public class SplitShipmentResponse {
    private SourceShipment sourceShipment; private List<ChildShipment> children; private int totalChildren;
    private long totalAllocatedQuantity; private String splitByName; private LocalDateTime splitAt;
    @Data @Builder public static class SourceShipment { private UUID id; private String name; private ShipmentStatus status; private long declaredQuantity; private long allocatedQuantity; }
    @Data @Builder public static class ChildShipment { private UUID id; private UUID parentShipmentId; private String name; private ShipmentStatus status; private PartnerOrganizationResponse recipientOrganization; private long totalQuantity; private String firstCode; private String lastCode; }
}
