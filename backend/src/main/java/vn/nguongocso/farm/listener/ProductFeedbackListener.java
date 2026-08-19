package vn.nguongocso.farm.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import vn.nguongocso.farm.event.ProductFeedbackSubmittedEvent;

@Component
public class ProductFeedbackListener {
    private static final Logger log = LoggerFactory.getLogger(ProductFeedbackListener.class);

    /**
     * Xử lý sự kiện ProductFeedbackSubmittedEvent khi người dùng gửi phản hồi về
     * sản phẩm.
     *
     * @param event Sự kiện ProductFeedbackSubmittedEvent chứa thông tin về phản hồi
     *              sản phẩm, lô sản xuất và tổ chức liên quan.
     */
    @Async
    @EventListener
    public void handleProductFeedbackSubmitted(ProductFeedbackSubmittedEvent event) {
        log.warn(
                "CẢNH BÁO PHẢN ÁNH SẢN PHẨM: Nhận được phản ánh mới về Lô sản xuất '{}' (ID: {}), thuộc tổ chức có ID: {}. Nội dung phản ánh: \"{}\"",
                event.getProductionLotName(),
                event.getProductionLotId(),
                event.getOrganizationId(),
                event.getContent());
    }
}
