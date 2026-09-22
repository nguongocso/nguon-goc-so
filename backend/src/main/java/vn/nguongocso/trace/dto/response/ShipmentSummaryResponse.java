package vn.nguongocso.trace.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import vn.nguongocso.trace.enums.ShipmentStatus;

/** DTO response tóm tắt lô hàng dùng khi tra cứu. */
@Data
@Builder
public class ShipmentSummaryResponse {
    private UUID id;

    private String name;

    private ShipmentStatus status;

    private String productionLotName;

    private Long totalQuantity;
}
