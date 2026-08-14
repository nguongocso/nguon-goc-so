package vn.nguongocso.trace.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import vn.nguongocso.trace.enums.ShipmentStatus;

/**
 * DTO rút gọn dùng khi tra cứu lô hàng bằng mã truy xuất (codeValue).
 * Trả về các trường cần thiết để frontend xác nhận lô hàng trước khi
 * ghi sự kiện thu mua.
 */
@Data
@Builder
public class ShipmentSummaryResponse {
    private UUID id;

    private String name;

    private ShipmentStatus status;

    private String productionLotName;

    private Long totalQuantity;
}