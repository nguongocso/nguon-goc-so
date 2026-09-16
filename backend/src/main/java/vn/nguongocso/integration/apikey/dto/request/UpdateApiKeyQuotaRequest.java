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
 * Hạn mức mới = hạn mức hiện tại + số lượt cộng thêm.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApiKeyQuotaRequest {

    /**
     * Số lượt cộng thêm vào hạn mức hiện tại. Phải lớn hơn 0.
     */
    @NotNull(message = "Số lượt cộng thêm không được để trống")
    @Min(value = 1, message = "Số lượt cộng thêm phải lớn hơn 0")
    private Integer incrementBy;
}