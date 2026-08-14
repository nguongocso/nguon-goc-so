package vn.nguongocso.farm.event;

import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Sự kiện được phát ra khi người dùng gửi phản hồi về sản phẩm.
 */
public class ProductFeedbackSubmittedEvent extends ApplicationEvent {
    private final UUID feedbackId;
    private final UUID productionLotId;
    private final String productionLotName;
    private final UUID organizationId;
    private final String content;

    public ProductFeedbackSubmittedEvent(Object source, UUID feedbackId, UUID productionLotId, String productionLotName, UUID organizationId, String content) {
        super(source);
        this.feedbackId = feedbackId;
        this.productionLotId = productionLotId;
        this.productionLotName = productionLotName;
        this.organizationId = organizationId;
        this.content = content;
    }

    /**
     * Lấy ID của phản hồi sản phẩm liên quan đến sự kiện.
     *
     * @return ID của phản hồi sản phẩm.
     */
    public UUID getFeedbackId() {
        return feedbackId;
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
     * Lấy tên của lô sản xuất liên quan đến sự kiện.
     *
     * @return Tên của lô sản xuất.
     */
    public String getProductionLotName() {
        return productionLotName;
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
     * Lấy nội dung phản hồi sản phẩm liên quan đến sự kiện.
     *
     * @return Nội dung phản hồi sản phẩm.
     */
    public String getContent() {
        return content;
    }
}
