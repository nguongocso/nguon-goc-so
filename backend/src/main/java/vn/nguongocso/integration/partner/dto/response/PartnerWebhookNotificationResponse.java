package vn.nguongocso.integration.partner.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus;

/**
 * DTO phản hồi thông tin và lịch sử phân phối thông báo Webhook thu hồi lô.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerWebhookNotificationResponse {
    private UUID id;

    private UUID partnerApiKeyId;

    private String partnerName;

    private UUID shipmentId;

    private String lotCode;

    private String newStatus;

    private String targetUrl;

    private String publicReason;

    private WebhookDeliveryStatus deliveryStatus;

    private Integer attemptCount;

    private Integer maxAttempts;

    private LocalDateTime nextRetryAt;

    private Integer lastHttpStatus;

    private String lastErrorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private List<PartnerWebhookAttemptDto> attempts;
}
