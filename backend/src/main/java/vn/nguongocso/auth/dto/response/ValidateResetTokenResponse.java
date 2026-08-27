package vn.nguongocso.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi kết quả kiểm tra tính hợp lệ của token đặt lại mật khẩu (NCL-01-CN-008).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateResetTokenResponse {

    private boolean valid;
    private String message;
}
