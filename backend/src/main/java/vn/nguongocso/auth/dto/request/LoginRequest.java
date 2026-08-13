package vn.nguongocso.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Yêu cầu đăng nhập, bao gồm tên đăng nhập, mật khẩu và mã tổ chức.
 */
@Data
public class LoginRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}