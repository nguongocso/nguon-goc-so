package vn.nguongocso.integration.apikey.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiKeyRequest {

    @NotBlank(message = "Tên đối tác không được để trống")
    @Size(max = 255, message = "Tên đối tác không vượt quá 255 ký tự")
    private String partnerName;

    @NotNull(message = "Hạn mức số lượt gọi trong 1 giờ không được để trống")
    @Min(value = 1, message = "Hạn mức số lượt gọi trong 1 giờ phải lớn hơn 0")
    private Integer rateLimitPerHour;

    @NotNull(message = "Ngày hết hạn không được để trống")
    @Future(message = "Ngày hết hạn phải ở thời điểm tương lai")
    private LocalDateTime expiresAt;
}
