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
 * Request gia hạn khóa truy cập (NCL-12-CN-005).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewApiKeyRequest {

    /**
     * Thời hạn mới của khóa. Phải ở thời điểm tương lai.
     */
    @NotNull(message = "Ngày hết hạn mới không được để trống")
    @Future(message = "Ngày hết hạn mới phải ở thời điểm tương lai")
    private LocalDateTime expiresAt;
}
