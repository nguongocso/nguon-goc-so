package vn.nguongocso.integration.partner.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import vn.nguongocso.integration.partner.entity.PartnerWebhookNotification;
import vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus;
import vn.nguongocso.integration.partner.repository.PartnerWebhookNotificationRepository;
import vn.nguongocso.certification.entity.SystemConfiguration;
import vn.nguongocso.certification.repository.SystemConfigurationRepository;
import vn.nguongocso.trace.entity.Shipment;

/**
 * Service điều phối và gửi thông báo Webhook tự động tới bên thứ ba khi lô bị thu hồi (NCL-12-CN-006).
 * <p>
 * Bắt sự kiện khi lô chuyển sang {@code RECALLING} hoặc {@code RECALLED}, xác định đúng đối tác
 * đã từng lấy dữ liệu lô trong khoảng thời gian cấu hình (TC-03), kiểm tra trạng thái khóa (TC-04),
 * gửi thông báo qua HTTPS và thực thi cơ chế thử lại theo lịch giãn dần (TC-01, TC-02).
 */
@Service
@RequiredArgsConstructor
public class PartnerRecallWebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PartnerRecallWebhookDispatcher.class);

    private static final String CONFIG_WINDOW_DAYS = "PARTNER_RECALL_NOTIFICATION_WINDOW_DAYS";
    private static final int DEFAULT_WINDOW_DAYS = 30;
    private static final int[] RETRY_INTERVAL_MINUTES = {1, 5, 15, 30, 60};

    private final PartnerWebhookDeliveryService webhookDeliveryService;
    private final PartnerWebhookNotificationRepository partnerWebhookNotificationRepository;
    private final SystemConfigurationRepository systemConfigurationRepository;

    /**
     * Điều phối gửi thông báo thu hồi cho danh sách các lô hàng.
     *
     * @param shipments         Danh sách lô hàng bị thu hồi
     * @param newStatus         Trạng thái mới: RECALLING hoặc RECALLED
     * @param publicReason      Lý do thu hồi ở mức công khai
     * @param remediationSummary Tóm tắt biện pháp khắc phục (nếu có, khi đóng case)
     */
    @Async
    public void dispatchRecallNotifications(
            List<Shipment> shipments,
            String newStatus,
            String publicReason,
            String remediationSummary) {

        if (shipments == null || shipments.isEmpty()) {
            return;
        }

        int windowDays = resolveNotificationWindowDays();
        LocalDateTime since = LocalDateTime.now().minusDays(windowDays);

        for (Shipment shipment : shipments) {
            try {
                webhookDeliveryService.processShipmentRecall(
                        shipment, newStatus, publicReason, remediationSummary, since);
            } catch (Exception e) {
                log.error("Lỗi khi điều phối thông báo thu hồi cho lô hàng shipmentId={}: {}",
                        shipment.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Xử lý xác định đối tác và tạo thông báo cho một lô hàng (ủy quyền sang DeliveryService).
     */
    public void processShipmentRecallNotification(
            Shipment shipment,
            String newStatus,
            String publicReason,
            String remediationSummary,
            LocalDateTime since) {
        webhookDeliveryService.processShipmentRecall(
                shipment, newStatus, publicReason, remediationSummary, since);
    }

    /**
     * Thực thi một lượt gửi HTTP Webhook POST (ủy quyền sang DeliveryService).
     */
    public void executeWebhookDelivery(PartnerWebhookNotification notification, String webhookSecret) {
        webhookDeliveryService.executeWebhookDelivery(notification, webhookSecret);
    }

    /**
     * Cron định kỳ quét các thông báo Webhook cần thử lại theo lịch giãn dần (NCL-12-CN-006-CV-04).
     */
    @Scheduled(fixedDelay = 60000)
    public void retryPendingNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<PartnerWebhookNotification> pendingList = partnerWebhookNotificationRepository
                .findByDeliveryStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                        WebhookDeliveryStatus.PENDING_RETRY, now);

        if (pendingList.isEmpty()) {
            return;
        }

        log.info("Tìm thấy {} thông báo Webhook đang chờ thử lại giãn dần", pendingList.size());
        for (PartnerWebhookNotification notification : pendingList) {
            try {
                String secret = notification.getPartnerApiKey() != null
                        ? notification.getPartnerApiKey().getWebhookSecret()
                        : null;
                webhookDeliveryService.executeWebhookDelivery(notification, secret);
            } catch (Exception e) {
                log.error("Lỗi khi thử lại gửi webhook deliveryId={}: {}", notification.getId(), e.getMessage());
            }
        }
    }

    private int resolveNotificationWindowDays() {
        try {
            Optional<SystemConfiguration> configOpt = systemConfigurationRepository.findById(CONFIG_WINDOW_DAYS);
            if (configOpt.isPresent()) {
                return Integer.parseInt(configOpt.get().getConfigValue().trim());
            }
        } catch (Exception e) {
            log.warn("Không thể đọc cấu hình {}: {}", CONFIG_WINDOW_DAYS, e.getMessage());
        }
        return DEFAULT_WINDOW_DAYS;
    }

    public static String computeHmacSha256(String data, String secret) {
        return PartnerWebhookDeliveryService.computeHmacSha256(data, secret);
    }
}
