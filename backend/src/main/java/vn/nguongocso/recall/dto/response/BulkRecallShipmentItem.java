package vn.nguongocso.recall.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.trace.recall.enums.LotResolution;

/**
 * Response DTO cho chi tiết lô hàng trong yêu cầu thu hồi hàng loạt (NCL-08-CN-011, NCL-08-CN-012).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkRecallShipmentItem {

    private UUID id;
    private UUID shipmentId;
    private String shipmentCode;
    private String shipmentName;
    private String shipmentStatus;
    private boolean included;
    private String exclusionReason;
    private String unit;
    private Long totalQuantity;
    private LotResolution resolution;
    private BigDecimal recoveredQuantity;
    private String notes;
}
