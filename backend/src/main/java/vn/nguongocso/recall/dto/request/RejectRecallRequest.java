package vn.nguongocso.recall.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Yêu cầu từ chối thu hồi lô sản xuất. */
@Getter
@Setter
@NoArgsConstructor
public class RejectRecallRequest {
    @NotBlank(message = "Lý do từ chối không được để trống.")
    @Size(max = 1000, message = "Lý do từ chối không được vượt quá 1000 ký tự.")
    private String rejectionReason;
}
