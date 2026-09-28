package vn.nguongocso.trace.recall.dto.response;

import lombok.Data;
import vn.nguongocso.trace.recall.enums.LotResolution;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** DTO response kết quả xử lý lô hàng trong vụ việc thu hồi. */
@Data
public class RecallLotResultResponse {
    private UUID id;

    private UUID shipmentId;

    private String shipmentName;

    private String unit;

    private LotResolution resolution;

    private BigDecimal recoveredQuantity;

    private String notes;

    private LocalDateTime createdAt;
}
