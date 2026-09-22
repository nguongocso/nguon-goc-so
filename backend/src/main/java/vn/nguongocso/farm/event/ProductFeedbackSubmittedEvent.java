package vn.nguongocso.farm.event;

import java.util.UUID;

import org.springframework.context.ApplicationEvent;

/**
 * Sự kiện phát ra khi người dùng gửi phản hồi về sản phẩm.
*/
public class ProductFeedbackSubmittedEvent extends ApplicationEvent {
    private final UUID feedbackId;

    private final UUID productionLotId;

    private final String productionLotName;

    private final UUID organizationId;

    private final String content;

    /** Khởi tạo sự kiện phản hồi sản phẩm được gửi. */
    public ProductFeedbackSubmittedEvent(Object source, UUID feedbackId, UUID productionLotId, String productionLotName,
            UUID organizationId, String content) {
        super(source);
        this.feedbackId = feedbackId;
        this.productionLotId = productionLotId;
        this.productionLotName = productionLotName;
        this.organizationId = organizationId;
        this.content = content;
    }

    /** Lấy mã phản hồi. */
    public UUID getFeedbackId() {
        return feedbackId;
    }

    /** Lấy mã lô sản xuất. */
    public UUID getProductionLotId() {
        return productionLotId;
    }

    /** Lấy tên lô sản xuất. */
    public String getProductionLotName() {
        return productionLotName;
    }

    /** Lấy mã tổ chức. */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /** Lấy nội dung phản hồi. */
    public String getContent() {
        return content;
    }
}
