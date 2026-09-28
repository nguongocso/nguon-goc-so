package vn.nguongocso.integration.apikey.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu nâng hạn mức khóa truy cập.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApiKeyQuotaRequest {
    @NotNull(message = "Số lượt hạn mức bổ sung không được để trống")
    @Min(value = 1, message = "Số lượt hạn mức bổ sung phải lớn hơn 0")
    private Integer incrementBy;
}
