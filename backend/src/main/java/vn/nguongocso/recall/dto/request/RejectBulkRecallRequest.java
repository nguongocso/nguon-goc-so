package vn.nguongocso.recall.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO để từ chối yêu cầu thu hồi hàng loạt (NCL-08-CN-011).
 */
@Getter
@Setter
public class RejectBulkRecallRequest {

    /**
     * Lý do từ chối (bắt buộc).
     */
    @NotBlank(message = "Lý do từ chối không được để trống.")
    private String reason;
}
