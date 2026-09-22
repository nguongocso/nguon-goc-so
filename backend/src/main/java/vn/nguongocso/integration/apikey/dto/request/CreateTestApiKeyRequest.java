package vn.nguongocso.integration.apikey.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu cấp mới khóa thử nghiệm (Sandbox/Test Key) dành cho đối tác bên thứ ba.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestApiKeyRequest {
    @Size(max = 255, message = "Tên đối tác không vượt quá 255 ký tự")
    private String partnerName;

    @Min(value = 1, message = "Hạn mức số lượt gọi trong 1 giờ phải lớn hơn 0")
    @Max(value = 50, message = "Hạn mức số lượt gọi thử nghiệm không vượt quá 50 lượt/giờ")
    private Integer rateLimitPerHour;

    @Future(message = "Ngày hết hạn phải ở thời điểm tương lai")
    private LocalDateTime expiresAt;

    public void setName(String name) {
        if (this.partnerName == null || this.partnerName.isBlank()) {
            this.partnerName = name;
        }
    }

    public void setExpireDays(Integer expireDays) {
        if (this.expiresAt == null && expireDays != null && expireDays > 0) {
            this.expiresAt = LocalDateTime.now().plusDays(expireDays);
        }
    }

    public void setRateLimit(Integer rateLimit) {
        if (this.rateLimitPerHour == null) {
            this.rateLimitPerHour = rateLimit;
        }
    }
}
