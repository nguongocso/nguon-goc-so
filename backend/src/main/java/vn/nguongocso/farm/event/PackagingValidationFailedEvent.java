package vn.nguongocso.farm.event;

import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Sự kiện được phát ra khi xác thực đóng gói thất bại.
 */
public class PackagingValidationFailedEvent extends ApplicationEvent {
    private final UUID productionLotId;
    private final UUID organizationId;
    private final String lotName;

    /**
     * Tạo một sự kiện xác thực đóng gói thất bại.
     *
     * @param source          Nguồn phát ra sự kiện.
     * @param productionLotId ID của lô sản xuất liên quan.
     * @param organizationId  ID của tổ chức liên quan.
     * @param lotName         Tên của lô sản xuất.
     */
    public PackagingValidationFailedEvent(Object source, UUID productionLotId, UUID organizationId, String lotName) {
        super(source);
        this.productionLotId = productionLotId;
        this.organizationId = organizationId;
        this.lotName = lotName;
    }

    /**
     * Lấy ID của lô sản xuất liên quan đến sự kiện.
     *
     * @return ID của lô sản xuất.
     */
    public UUID getProductionLotId() {
        return productionLotId;
    }

    /**
     * Lấy ID của tổ chức liên quan đến sự kiện.
     *
     * @return ID của tổ chức.
     */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /**
     * Lấy tên của lô sản xuất liên quan đến sự kiện.
     *
     * @return Tên của lô sản xuất.
     */
    public String getLotName() {
        return lotName;
    }
}
