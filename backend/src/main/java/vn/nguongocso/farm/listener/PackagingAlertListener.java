package vn.nguongocso.farm.listener;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import vn.nguongocso.farm.event.PackagingValidationFailedEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PackagingAlertListener {
    private static final Logger log = LoggerFactory.getLogger(PackagingAlertListener.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @EventListener
    public void handlePackagingValidationFailed(PackagingValidationFailedEvent event) {
        log.warn("CẢNH BÁO THIẾU NHẬT KÝ: Lô sản xuất '{}' (ID: {}) thuộc tổ chức ID {} không đủ điều kiện đóng gói.",
                event.getLotName(), event.getProductionLotId(), event.getOrganizationId());

        String destination = "/topic/notifications/" + event.getOrganizationId();
        messagingTemplate.convertAndSend(destination, Map.of(
                "type", "PACKAGING_FAILED",
                "productionLotId", event.getProductionLotId().toString(),
                "lotName", event.getLotName(),
                "message", "Cảnh báo: Lô sản xuất '" + event.getLotName() + "' không đủ điều kiện đóng gói do thiếu nhật ký bắt buộc."
        ));
    }
}
