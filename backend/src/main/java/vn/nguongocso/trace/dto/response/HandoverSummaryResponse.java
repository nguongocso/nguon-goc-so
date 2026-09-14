package vn.nguongocso.trace.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;

/**
 * Response DTO tóm tắt phiếu bàn giao phục vụ danh sách VT-04.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HandoverSummaryResponse {

    private UUID id;
    private UUID shipmentId;
    private String shipmentName;
    private String fromOrganizationName;
    private String toOrganizationName;
    private Long quantity;
    private String unit;
    private ShipmentHandoverStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private String rejectionReason;
}
