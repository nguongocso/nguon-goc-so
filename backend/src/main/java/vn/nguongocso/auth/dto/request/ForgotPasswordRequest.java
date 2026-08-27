package vn.nguongocso.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO tiếp nhận yêu cầu quên mật khẩu của người dùng (NCL-01-CN-008).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest {

    @NotBlank(message = "Vui lòng nhập tên đăng nhập hoặc email")
    private String emailOrUsername;
}
