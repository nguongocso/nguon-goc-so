package vn.nguongocso.organization.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Yêu cầu chấp nhận lời mời tham gia tổ chức.
 */
@Getter
@Setter
public class AcceptInvitationRequest {
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Size(min = 4, max = 30, message = "Tên đăng nhập phải có từ 4 đến 30 ký tự")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Tên đăng nhập chỉ chứa chữ cái, chữ số, dấu gạch ngang và dấu gạch dưới")
        private String userName;

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 50, message = "Mật khẩu phải từ 8 đến 50 ký tự")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", message = "Mật khẩu phải chứa ít nhất một chữ hoa, một chữ thường, một số và một ký tự đặc biệt")
        private String password;

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        private String fullName;

        @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d(\\s|\\.)?){7}$", message = "Số điện thoại không hợp lệ")
        private String phone;
}
