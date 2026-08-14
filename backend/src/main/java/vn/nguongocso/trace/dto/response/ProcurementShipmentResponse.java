package vn.nguongocso.trace.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import vn.nguongocso.trace.enums.ShipmentStatus;

/**
 * DTO dành riêng cho màn hình "Thu mua nông sản" của Doanh nghiệp thu mua
 * (VT‑04).
 * Chỉ chứa các trường cần thiết để hiển thị danh sách lô hàng sẵn sàng thu mua.
 */
@Data
@Builder
public class ProcurementShipmentResponse {
    private UUID id;

    private String name;

    private ShipmentStatus status;

    private String productionLotName;

    private String productCategoryName;

    private Long totalQuantity;
}