package vn.nguongocso.trace.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelTraceCodesResponse {
    private UUID shipmentId;
    private int totalCancelled;
    private long refundedQuota;
    private long remainingQuota;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private String message;
}
