package vn.nguongocso.trace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Yêu cầu từ chối cấp bổ sung dải mã truy xuất. */
@Getter
@Setter
@NoArgsConstructor
public class RejectSupplementRequest {
    @NotBlank(message = "Lý do từ chối không được để trống.")
    @Size(max = 1000, message = "Lý do từ chối không được vượt quá 1000 ký tự.")
    private String rejectionReason;
}
