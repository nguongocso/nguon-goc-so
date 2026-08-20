package vn.nguongocso.farm.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import vn.nguongocso.farm.event.PackagingValidationFailedEvent;

/**
 * Lớp PackagingAlertListener lắng nghe sự kiện PackagingValidationFailedEvent
 * và xử lý cảnh báo khi xác thực đóng gói thất bại.
 */
@Component
public class PackagingAlertListener {
    private static final Logger log = LoggerFactory.getLogger(PackagingAlertListener.class);

    /**
     * Xử lý sự kiện PackagingValidationFailedEvent khi xác thực đóng gói thất bại.
     *
     * @param event Sự kiện PackagingValidationFailedEvent chứa thông tin về lô sản
     *              xuất và tổ chức liên quan.
     */
    @Async
    @EventListener
    public void handlePackagingValidationFailed(PackagingValidationFailedEvent event) {
        log.warn("CẢNH BÁO THIẾU NHẬT KÝ: Lô sản xuất '{}' (ID: {}) thuộc tổ chức ID {} không đủ điều kiện đóng gói.",
                event.getLotName(), event.getProductionLotId(), event.getOrganizationId());

    }
}
