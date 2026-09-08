package vn.nguongocso.trace.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;

/**
 * Response DTO cho phiếu bàn giao.
 */
@Getter
@Builder
public class HandoverResponse {

    private UUID id;
    private UUID shipmentId;
    private String shipmentName;
    private UUID fromOrganizationId;
    private String fromOrganizationName;
    private UUID toOrganizationId;
    private String toOrganizationName;
    private Long quantity;
    private ShipmentHandoverStatus status;
    private LocalDateTime plannedAt;
    private String vehicleInfo;
    private String carrierName;
    private String note;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private UUID confirmedBy;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
    private UUID rejectedBy;
    private String rejectedByName;
    private LocalDateTime rejectedAt;
    private String cancelReason;
    private UUID cancelledBy;
    private String cancelledByName;
    private LocalDateTime cancelledAt;
}
