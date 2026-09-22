package vn.nguongocso.integration.partner.dto.request;

import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO yêu cầu đăng ký hoặc cập nhật địa chỉ nhận thông báo Webhook.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerWebhookRegistrationRequest {
    @Size(max = 500, message = "Địa chỉ nhận thông báo không được vượt quá 500 ký tự")
    private String webhookUrl;

    @Builder.Default
    private Boolean isActive = true;
}
