package vn.nguongocso.trace.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload duyệt yêu cầu cấp bổ sung dải mã truy xuất (NCL-04-CN-007).
 *
 * <p>Duyệt toàn bộ khi {@code approvedQuantity} bằng số lượng đề nghị;
 * duyệt một phần khi nhỏ hơn. Không được vượt quá số lượng đề nghị.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class ApproveSupplementRequest {

    @NotNull(message = "Số lượng thực cấp không được để trống.")
    @Min(value = 1, message = "Số lượng thực cấp phải lớn hơn 0.")
    private Long approvedQuantity;

    @Size(max = 2000, message = "Ghi chú không được vượt quá 2000 ký tự.")
    private String remarks;
}
