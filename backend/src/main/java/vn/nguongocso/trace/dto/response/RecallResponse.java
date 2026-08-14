package vn.nguongocso.trace.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi dành cho thông tin một đợt thu hồi lô hàng.
 */
@Getter
@Setter
@NoArgsConstructor
public class RecallResponse {
    private UUID id;

    private UUID shipmentId;

    private String reason;

    private UUID recalledBy;

    private LocalDateTime recalledAt;

    private String status;

    private String shipmentStatus;

    private Integer traceCodesUpdated;
}