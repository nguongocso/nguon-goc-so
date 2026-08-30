package vn.nguongocso.trace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu mở khóa mã tem sau khi xác minh (NCL-08-CN-013).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnlockTraceCodeRequest {

    @NotBlank(message = "Vui lòng nhập kết luận xác minh")
    @Size(min = 10, max = 500, message = "Kết luận xác minh phải từ 10 đến 500 ký tự")
    private String conclusion;

    @Size(max = 500, message = "Bằng chứng xác minh không được vượt quá 500 ký tự")
    private String evidence;
}
