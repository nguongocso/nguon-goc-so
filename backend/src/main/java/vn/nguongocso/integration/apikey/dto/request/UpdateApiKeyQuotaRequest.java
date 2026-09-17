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
 * Hạn mức mới = hạn mức hiện tại + số lượt hạn mức bổ sung.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApiKeyQuotaRequest {

    /**
     * Số lượt hạn mức bổ sung vào hạn mức hiện tại. Phải lớn hơn 0.
     */
    @NotNull(message = "Số lượt hạn mức bổ sung không được để trống")
    @Min(value = 1, message = "Số lượt hạn mức bổ sung phải lớn hơn 0")
    private Integer incrementBy;
}