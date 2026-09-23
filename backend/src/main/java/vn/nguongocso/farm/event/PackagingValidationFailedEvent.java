package vn.nguongocso.farm.event;

import java.util.UUID;

import org.springframework.context.ApplicationEvent;

/**
 * Sự kiện phát ra khi xác thực đóng gói thất bại.
*/
public class PackagingValidationFailedEvent extends ApplicationEvent {
    private final UUID productionLotId;
    private final UUID organizationId;
    private final String lotName;

    /** Tạo sự kiện xác thực đóng gói thất bại. */
    public PackagingValidationFailedEvent(Object source, UUID productionLotId, UUID organizationId, String lotName) {
        super(source);
        this.productionLotId = productionLotId;
        this.organizationId = organizationId;
        this.lotName = lotName;
    }

    /** Lấy mã lô sản xuất. */
    public UUID getProductionLotId() {
        return productionLotId;
    }

    /** Lấy mã tổ chức. */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /** Lấy tên lô sản xuất. */
    public String getLotName() {
        return lotName;
    }
}
