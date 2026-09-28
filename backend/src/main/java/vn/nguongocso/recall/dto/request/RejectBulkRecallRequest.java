package vn.nguongocso.recall.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Yêu cầu từ chối thu hồi hàng loạt. */
@Getter
@Setter
public class RejectBulkRecallRequest {
    @NotBlank(message = "Lý do từ chối không được để trống.")
    private String reason;
}
