package vn.nguongocso.integration.partner.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO đại diện cho một lần thử gửi thông báo Webhook.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerWebhookAttemptDto {
    private Integer attemptNumber;

    private LocalDateTime attemptedAt;

    private Integer httpStatus;

    private String responseBody;

    private String errorMessage;

    private Long durationMs;
}
