package vn.nguongocso.integration.apikey.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu gia hạn khóa truy cập.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewApiKeyRequest {
    @NotNull(message = "Ngày hết hạn mới không được để trống")
    @Future(message = "Ngày hết hạn mới phải ở thời điểm tương lai")
    private LocalDateTime expiresAt;
}
