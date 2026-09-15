package vn.nguongocso.integration.partner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO yêu cầu đăng ký hoặc cập nhật địa chỉ nhận thông báo Webhook (NCL-12-CN-006).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerWebhookRegistrationRequest {

    /** Địa chỉ URL nhận webhook, bắt buộc kết nối bảo mật HTTPS. */
    @NotBlank(message = "Địa chỉ nhận thông báo không được để trống")
    @Size(max = 500, message = "Địa chỉ nhận thông báo không được vượt quá 500 ký tự")
    private String webhookUrl;

    /** Trạng thái bật/tắt nhận thông báo qua webhook. */
    @Builder.Default
    private Boolean isActive = true;
}
