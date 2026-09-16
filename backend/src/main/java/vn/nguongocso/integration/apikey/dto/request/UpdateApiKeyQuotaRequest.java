package vn.nguongocso.integration.apikey.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request nâng hạn mức khóa truy cập (NCL-12-CN-005).
 * Chỉ chấp nhận giá trị mới lớn hơn giá trị hiện tại.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApiKeyQuotaRequest {

    /**
     * Hạn mức gọi mới (rateLimitPerHour). Phải lớn hơn giá trị hiện tại.
     */
    @NotNull(message = "Hạn mức mới không được để trống")
    @Min(value = 1, message = "Hạn mức phải lớn hơn 0")
    private Integer rateLimitPerHour;
}