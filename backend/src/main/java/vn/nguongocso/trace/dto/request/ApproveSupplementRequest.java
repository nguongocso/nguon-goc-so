package vn.nguongocso.trace.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Yêu cầu duyệt cấp bổ sung dải mã truy xuất. */
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
