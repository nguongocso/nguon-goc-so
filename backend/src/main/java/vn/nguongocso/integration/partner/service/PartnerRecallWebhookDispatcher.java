package vn.nguongocso.integration.partner.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.integration.partner.dto.response.PartnerRecallPayloadDto;
import vn.nguongocso.integration.partner.dto.response.PartnerWebhookAttemptDto;
import vn.nguongocso.integration.partner.entity.PartnerWebhookNotification;
import vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus;
import vn.nguongocso.integration.partner.repository.PartnerLotAccessLogRepository;
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

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final PartnerLotAccessLogRepository partnerLotAccessLogRepository;
    private final PartnerApiKeyRepository partnerApiKeyRepository;
    private final PartnerWebhookNotificationRepository partnerWebhookNotificationRepository;
    private final SystemConfigurationRepository systemConfigurationRepository;
    private final ObjectMapper objectMapper;

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
                processShipmentRecallNotification(shipment, newStatus, publicReason, remediationSummary, since);
            } catch (Exception e) {
                log.error("Lỗi khi điều phối thông báo thu hồi cho lô hàng shipmentId={}: {}",
                        shipment.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Xử lý xác định đối tác và tạo thông báo cho một lô hàng.
     */
    @Transactional
    public void processShipmentRecallNotification(
            Shipment shipment,
            String newStatus,
            String publicReason,
            String remediationSummary,
            LocalDateTime since) {

        UUID productionLotId = (shipment.getProductionLot() != null)
                ? shipment.getProductionLot().getId()
                : null;

        // 1. Tìm các ID khóa đối tác đã từng lấy dữ liệu lô này trong T ngày qua (TC-03)
        List<UUID> candidateApiKeyIds = partnerLotAccessLogRepository
                .findDistinctPartnerApiKeyIdsByShipmentOrProductionLot(shipment.getId(), productionLotId, since);

        if (candidateApiKeyIds.isEmpty()) {
            log.info("Không có đối tác bên thứ ba nào truy xuất dữ liệu của lô {} trong {} ngày qua - bỏ qua (TC-03).",
                    shipment.getName(), DEFAULT_WINDOW_DAYS);
            return;
        }

        // 2. Lọc các khóa đủ điều kiện: ACTIVE, chưa hết hạn, có webhookUrl HTTPS, bật nhận tin, không phải test (TC-04)
        List<PartnerApiKey> eligibleKeys = partnerApiKeyRepository
                .findEligibleWebhookKeys(candidateApiKeyIds, LocalDateTime.now());

        if (eligibleKeys.isEmpty()) {
            log.info("Lô {}: Tìm thấy {} đối tác đã truy xuất nhưng không có khóa nào đủ điều kiện nhận webhook.",
                    shipment.getName(), candidateApiKeyIds.size());
            return;
        }

        log.info("Phát hiện {} đối tác đủ điều kiện nhận thông báo thu hồi cho lô {}",
                eligibleKeys.size(), shipment.getName());

        for (PartnerApiKey apiKey : eligibleKeys) {
            // Kiểm tra trạng thái khóa: nếu đã bị thu hồi thì ngừng gửi (TC-04)
            if (apiKey.getStatus() == PartnerApiKeyStatus.REVOKED) {
                log.warn("Khóa của đối tác '{}' đã bị thu hồi - không gửi thông báo (TC-04).", apiKey.getPartnerName());
                continue;
            }

            createAndSendNotification(apiKey, shipment, newStatus, publicReason, remediationSummary);
        }
    }

    /**
     * Khởi tạo bản ghi thông báo và thực hiện lượt gửi đầu tiên.
     */
    private void createAndSendNotification(
            PartnerApiKey apiKey,
            Shipment shipment,
            String newStatus,
            String publicReason,
            String remediationSummary) {

        String shipmentCode = (shipment.getName() != null && !shipment.getName().isBlank())
                ? shipment.getName()
                : shipment.getId().toString();

        String lotCode = (shipment.getProductionLot() != null && shipment.getProductionLot().getName() != null)
                ? shipment.getProductionLot().getName()
                : null;

        UUID productionLotId = (shipment.getProductionLot() != null)
                ? shipment.getProductionLot().getId()
                : null;

        String productName = (shipment.getProductionLot() != null && shipment.getProductionLot().getProductCategory() != null)
                ? shipment.getProductionLot().getProductCategory().getName()
                : (shipment.getProductionLot() != null ? shipment.getProductionLot().getName() : "Sản phẩm");

        UUID eventId = UUID.randomUUID();
        PartnerRecallPayloadDto payloadDto = PartnerRecallPayloadDto.builder()
                .eventId(eventId)
                .eventType("RECALL_STATUS_CHANGED")
                .shipmentId(shipment.getId())
                .shipmentCode(shipmentCode)
                .productionLotId(productionLotId)
                .productionLotCode(lotCode)
                .productName(productName)
                .previousStatus("ACTIVATED")
                .newStatus(newStatus)
                .timestamp(LocalDateTime.now())
                .publicReason(publicReason)
                .remediationSummary(remediationSummary)
                .build();

        String payloadJson = serializePayload(payloadDto);

        PartnerWebhookNotification notification = PartnerWebhookNotification.builder()
                .partnerApiKey(apiKey)
                .shipment(shipment)
                .lotCode(shipmentCode)
                .newStatus(newStatus)
                .targetUrl(apiKey.getWebhookUrl())
                .publicReason(publicReason)
                .payload(payloadJson)
                .deliveryStatus(WebhookDeliveryStatus.PENDING_RETRY)
                .attemptCount(0)
                .maxAttempts(5)
                .createdAt(LocalDateTime.now())
                .build();

        PartnerWebhookNotification saved = partnerWebhookNotificationRepository.save(notification);

        // Bắn HTTP POST ngay lần 1
        executeWebhookDelivery(saved, apiKey.getWebhookSecret());
    }

    /**
     * Thực thi một lượt gửi HTTP Webhook POST kèm chữ ký số và cập nhật kết quả.
     */
    @Transactional
    public void executeWebhookDelivery(PartnerWebhookNotification notification, String webhookSecret) {
        // Kiểm tra điều kiện ngắt: Khóa đã bị thu hồi -> Hủy bỏ gửi ngay lập tức (TC-04)
        PartnerApiKey apiKey = notification.getPartnerApiKey();
        if (apiKey.getStatus() == PartnerApiKeyStatus.REVOKED) {
            notification.setDeliveryStatus(WebhookDeliveryStatus.CANCELLED);
            notification.setLastErrorMessage("Đã hủy gửi thông báo: Khóa truy cập đối tác đã bị thu hồi (TC-04).");
            notification.setCompletedAt(LocalDateTime.now());
            partnerWebhookNotificationRepository.save(notification);
            log.info("Đã hủy lượt gửi webhook deliveryId={} do khóa apiKeyId={} đã bị REVOKED (TC-04)",
                    notification.getId(), apiKey.getId());
            return;
        }

        int currentAttempt = notification.getAttemptCount() + 1;
        long startTime = System.currentTimeMillis();
        long epochTimestamp = Instant.now().getEpochSecond();
        String payloadJson = notification.getPayload();

        String signatureHeader = "";
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            String signedData = epochTimestamp + "." + payloadJson;
            String signature = computeHmacSha256(signedData, webhookSecret);
            signatureHeader = "t=" + epochTimestamp + ",v1=" + signature;
        }

        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(notification.getTargetUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("User-Agent", "NguonGocSo-Webhook/1.0")
                    .header("X-Webhook-Event", "RECALL_STATUS_CHANGED")
                    .header("X-Webhook-Delivery-Id", notification.getId().toString())
                    .header("X-Webhook-Timestamp", String.valueOf(epochTimestamp))
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8));

            if (!signatureHeader.isBlank()) {
                reqBuilder.header("X-Webhook-Signature", signatureHeader);
            }

            HttpResponse<String> response = HTTP_CLIENT.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = response.statusCode();
            boolean success = (statusCode >= 200 && statusCode < 300);

            String body = response.body();
            if (body != null && body.length() > 500) {
                body = body.substring(0, 500) + "...";
            }

            recordAttemptResult(notification, currentAttempt, statusCode, body, null, duration, success);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("Gửi webhook thất bại tới {} (lần thử {}): {}",
                    notification.getTargetUrl(), currentAttempt, e.getMessage());

            recordAttemptResult(notification, currentAttempt, null, null,
                    "Không thể kết nối tới đối tác: " + e.getMessage(), duration, false);
        }
    }

    /**
     * Ghi nhận kết quả của một lần thử gửi, tính toán lịch giãn dần nếu thất bại.
     */
    private void recordAttemptResult(
            PartnerWebhookNotification notification,
            int attemptNumber,
            Integer httpStatus,
            String responseBody,
            String errorMessage,
            long durationMs,
            boolean success) {

        List<PartnerWebhookAttemptDto> attempts = parseAttemptsLog(notification.getAttemptsLog());
        attempts.add(PartnerWebhookAttemptDto.builder()
                .attemptNumber(attemptNumber)
                .attemptedAt(LocalDateTime.now())
                .httpStatus(httpStatus)
                .responseBody(responseBody)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .build());

        notification.setAttemptsLog(serializeAttemptsLog(attempts));
        notification.setAttemptCount(attemptNumber);
        notification.setLastHttpStatus(httpStatus);
        notification.setLastErrorMessage(errorMessage);

        if (success) {
            // Thành công (TC-01)
            notification.setDeliveryStatus(WebhookDeliveryStatus.SUCCESS);
            notification.setNextRetryAt(null);
            notification.setCompletedAt(LocalDateTime.now());
            log.info("Bắn webhook thành công tới {} (lần thử {})", notification.getTargetUrl(), attemptNumber);
        } else {
            // Thất bại -> Lập lịch thử lại giãn dần (TC-02)
            if (attemptNumber >= notification.getMaxAttempts()) {
                notification.setDeliveryStatus(WebhookDeliveryStatus.FAILED);
                notification.setNextRetryAt(null);
                notification.setCompletedAt(LocalDateTime.now());
                log.warn("Bắn webhook thất bại vĩnh viễn tới {} sau {} lần thử",
                        notification.getTargetUrl(), attemptNumber);
            } else {
                int delayMinutes = RETRY_INTERVAL_MINUTES[Math.min(attemptNumber - 1, RETRY_INTERVAL_MINUTES.length - 1)];
                notification.setDeliveryStatus(WebhookDeliveryStatus.PENDING_RETRY);
                notification.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
                log.info("Lên lịch thử lại lần {} tới {} sau {} phút",
                        attemptNumber + 1, notification.getTargetUrl(), delayMinutes);
            }
        }

        partnerWebhookNotificationRepository.save(notification);
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
                String secret = notification.getPartnerApiKey().getWebhookSecret();
                executeWebhookDelivery(notification, secret);
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
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String serializePayload(PartnerRecallPayloadDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String serializeAttemptsLog(List<PartnerWebhookAttemptDto> attempts) {
        try {
            return objectMapper.writeValueAsString(attempts);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<PartnerWebhookAttemptDto> parseAttemptsLog(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<PartnerWebhookAttemptDto>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
