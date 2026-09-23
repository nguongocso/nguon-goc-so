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
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.integration.partner.dto.response.PartnerRecallPayloadDto;
import vn.nguongocso.integration.partner.dto.response.PartnerWebhookAttemptDto;
import vn.nguongocso.integration.partner.entity.PartnerWebhookNotification;
import vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus;
import vn.nguongocso.integration.partner.repository.PartnerLotAccessLogRepository;
import vn.nguongocso.integration.partner.repository.PartnerWebhookNotificationRepository;
import vn.nguongocso.trace.entity.Shipment;

/**
 * Service xử lý tạo bản ghi và thực thi phân phối Webhook tới đối tác bên thứ ba.
*/
@Service
@RequiredArgsConstructor
public class PartnerWebhookDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(PartnerWebhookDeliveryService.class);
    private static final int[] RETRY_INTERVAL_MINUTES = {1, 5, 15, 30, 60};
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final PartnerLotAccessLogRepository partnerLotAccessLogRepository;
    private final PartnerApiKeyRepository partnerApiKeyRepository;
    private final PartnerWebhookNotificationRepository partnerWebhookNotificationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Xử lý xác định đối tác đủ điều kiện và khởi tạo thông báo thu hồi cho một lô hàng.
     */
    @Transactional
    public void processShipmentRecall(
            Shipment shipment,
            String newStatus,
            String publicReason,
            String remediationSummary,
            LocalDateTime since) {

        UUID productionLotId = (shipment.getProductionLot() != null)
                ? shipment.getProductionLot().getId()
                : null;

        // 1. Tìm các ID khóa đối tác đã từng lấy dữ liệu lô này trong cửa sổ cấu hình (TC-03)
        List<UUID> candidateApiKeyIds = partnerLotAccessLogRepository
                .findDistinctPartnerApiKeyIdsByShipmentOrProductionLot(shipment.getId(), productionLotId, since);

        if (candidateApiKeyIds.isEmpty()) {
            log.info("Không có đối tác bên thứ ba nào truy xuất dữ liệu của lô {} trong cửa sổ cấu hình.",
                    shipment.getName());
            return;
        }

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
            if (apiKey.getStatus() == PartnerApiKeyStatus.REVOKED) {
                log.warn("Khóa của đối tác '{}' đã bị thu hồi.", apiKey.getPartnerName());
                continue;
            }

            createAndSendNotification(apiKey, shipment, newStatus, publicReason, remediationSummary);
        }
    }

    /**
     * Khởi tạo bản ghi thông báo và thực hiện lượt gửi đầu tiên.
     */
    @Transactional
    public void createAndSendNotification(
            PartnerApiKey apiKey,
            Shipment shipment,
            String newStatus,
            String publicReason,
            String remediationSummary) {

        if (partnerWebhookNotificationRepository.existsByPartnerApiKey_IdAndShipment_IdAndNewStatus(
                apiKey.getId(), shipment.getId(), newStatus)) {
            log.info("Bỏ qua thông báo trùng lặp: đối tác apiKeyId={} đã được gửi thông báo cho lô shipmentId={} trạng thái={}",
                    apiKey.getId(), shipment.getId(), newStatus);
            return;
        }

        String shipmentCode = (shipment.getName() != null && !shipment.getName().isBlank())
                ? shipment.getName()
                : shipment.getId().toString();

        String lotCode = (shipment.getProductionLot() != null && shipment.getProductionLot().getName() != null)
                ? shipment.getProductionLot().getName()
                : null;

        UUID productionLotId = (shipment.getProductionLot() != null)
                ? shipment.getProductionLot().getId()
                : null;

        String productName = (shipment.getProductionLot() != null
                        && shipment.getProductionLot().getProductCategory() != null)
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
                .publicReason(publicReason != null ? publicReason : "Thông báo thu hồi lô hàng")
                .remediationSummary(remediationSummary)
                .build();

        String payloadJson = serializePayload(payloadDto);

        PartnerWebhookNotification notification = PartnerWebhookNotification.builder()
                .partnerApiKey(apiKey)
                .shipment(shipment)
                .lotCode(shipmentCode)
                .newStatus(newStatus)
                .targetUrl(apiKey.getWebhookUrl())
                .publicReason(publicReason != null ? publicReason : "Thông báo thu hồi lô hàng")
                .payload(payloadJson)
                .deliveryStatus(WebhookDeliveryStatus.PENDING_RETRY)
                .attemptCount(0)
                .maxAttempts(5)
                .createdAt(LocalDateTime.now())
                .build();

        PartnerWebhookNotification saved = partnerWebhookNotificationRepository.save(notification);

        executeWebhookDelivery(saved, apiKey.getWebhookSecret());
    }

    /**
     * Thực thi một lượt gửi HTTP Webhook kèm chữ ký số và ghi nhận kết quả.
     */
    @Transactional
    public void executeWebhookDelivery(PartnerWebhookNotification notification, String webhookSecret) {
        PartnerApiKey apiKey = notification.getPartnerApiKey();
        if (apiKey.getStatus() == PartnerApiKeyStatus.REVOKED) {
            notification.setDeliveryStatus(WebhookDeliveryStatus.CANCELLED);
            notification.setLastErrorMessage("Đã hủy gửi thông báo: Khóa truy cập đối tác đã bị thu hồi (TC-04).");
            notification.setCompletedAt(LocalDateTime.now());
            partnerWebhookNotificationRepository.save(notification);
            log.info("Đã hủy lượt gửi webhook deliveryId={} do khóa apiKeyId={} đã bị thu hồi.",
                    notification.getId(), apiKey.getId());
            return;
        }

        int currentAttempt = notification.getAttemptCount() + 1;
        long startNano = System.nanoTime();

        try {
            long timestampEpoch = Instant.now().getEpochSecond();
            String payload = notification.getPayload();
            String signedData = timestampEpoch + "." + payload;
            String signature = computeHmacSha256(signedData, webhookSecret);
            String signatureHeader = "t=" + timestampEpoch + ",v1=" + signature;

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(notification.getTargetUrl()))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("User-Agent", "AgriTrace-Webhook-Dispatcher/1.0")
                    .header("X-Webhook-Event", "RECALL_NOTIFICATION")
                    .header("X-Webhook-Delivery-Id", notification.getId().toString())
                    .header("X-Webhook-Signature", signatureHeader)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));

            HttpRequest httpRequest = reqBuilder.build();
            HttpResponse<String> response = HTTP_CLIENT.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long durationMs = Duration.ofNanos(System.nanoTime() - startNano).toMillis();

            int statusCode = response.statusCode();
            String responseBody = response.body();
            if (responseBody != null && responseBody.length() > 500) {
                responseBody = responseBody.substring(0, 500) + "...";
            }

            boolean isSuccess = (statusCode >= 200 && statusCode < 300);
            String errorMsg = isSuccess ? null : ("Máy chủ đối tác phản hồi HTTP " + statusCode);

            recordAttemptResult(notification, currentAttempt, statusCode, responseBody, errorMsg, durationMs,
                    isSuccess);

        } catch (Exception e) {
            long durationMs = Duration.ofNanos(System.nanoTime() - startNano).toMillis();
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }

            log.warn("Gửi webhook thất bại tới {} (lần thử {}): {}",
                    notification.getTargetUrl(), currentAttempt, errorMsg);

            recordAttemptResult(notification, currentAttempt, null, null, errorMsg, durationMs, false);
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
            notification.setDeliveryStatus(WebhookDeliveryStatus.SUCCESS);
            notification.setNextRetryAt(null);
            notification.setCompletedAt(LocalDateTime.now());
            log.info("Bắn webhook thành công tới {} (lần thử {})", notification.getTargetUrl(), attemptNumber);
        } else {
            if (attemptNumber >= notification.getMaxAttempts()) {
                notification.setDeliveryStatus(WebhookDeliveryStatus.FAILED);
                notification.setNextRetryAt(null);
                notification.setCompletedAt(LocalDateTime.now());
                log.warn("Bắn webhook thất bại vĩnh viễn tới {} sau {} lần thử",
                        notification.getTargetUrl(), attemptNumber);
            } else {
                int delayMinutes = RETRY_INTERVAL_MINUTES[Math.min(attemptNumber - 1,
                        RETRY_INTERVAL_MINUTES.length - 1)];
                notification.setDeliveryStatus(WebhookDeliveryStatus.PENDING_RETRY);
                notification.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
                log.info("Lên lịch thử lại lần {} tới {} sau {} phút",
                        attemptNumber + 1, notification.getTargetUrl(), delayMinutes);
            }
        }

        partnerWebhookNotificationRepository.save(notification);
    }

    /**
     * Tính toán chữ ký của dữ liệu gửi webhook.
     */
    public static String computeHmacSha256(String data, String secret) {
        if (secret == null || secret.isBlank()) {
            return "";
        }
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Lỗi khi tính toán HMAC-SHA256: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Chuyển đổi payload thông báo sang chuỗi JSON.
     */
    private String serializePayload(PartnerRecallPayloadDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Chuyển đổi danh sách các lần thử gửi sang chuỗi JSON.
     */
    private String serializeAttemptsLog(List<PartnerWebhookAttemptDto> attempts) {
        try {
            return objectMapper.writeValueAsString(attempts);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * Phân tích danh sách các lần thử gửi từ chuỗi JSON.
     */
    public List<PartnerWebhookAttemptDto> parseAttemptsLog(String json) {
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
