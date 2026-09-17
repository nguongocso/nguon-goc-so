package vn.nguongocso.integration.partner.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi thông tin cấu hình địa chỉ nhận thông báo Webhook của đối tác (NCL-12-CN-006).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerWebhookResponse {

    private UUID id;
    private String partnerName;
    private String keyPrefix;
    private String webhookUrl;
    private Boolean isWebhookActive;
    private String webhookSecret;
    private LocalDateTime updatedAt;
}
