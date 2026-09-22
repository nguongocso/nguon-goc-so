package vn.nguongocso.trace.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import vn.nguongocso.trace.enums.ShipmentStatus;

/** DTO response danh sách lô hàng sẵn sàng thu mua. */
@Data
@Builder
public class ProcurementShipmentResponse {
    private UUID id;

    private String name;

    private ShipmentStatus status;

    private String productionLotName;

    private String productCategoryName;

    private String organizationName;

    private UUID cooperativeOrganizationId;

    private Long totalQuantity;
}