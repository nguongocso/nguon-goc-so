package vn.nguongocso.organization.dto.request;

import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.organization.enums.OrganizationType;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;

/**
 * DTO chứa thông tin yêu cầu tạo mới tổ chức.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizationRequest {
        // Thông tin tổ chức
        @NotBlank(message = "Tên tổ chức không được để trống")
        @Size(max = 255, message = "Tên tổ chức không được vượt quá 255 ký tự")
        private String organizationName;

        @NotBlank(message = "Mã tổ chức không được để trống")
        @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Mã tổ chức chỉ được chứa chữ in hoa, số, dấu gạch ngang và gạch dưới")
        private String organizationCode;

        @NotNull(message = "Loại tổ chức không được để trống")
        private OrganizationType organizationType;

        @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
        private String address;

        @Pattern(regexp = "^(0|\\+84)[1-9][0-9]{8}$", message = "Số điện thoại tổ chức không hợp lệ")
        private String phone;

        @Email(message = "Email tổ chức không đúng định dạng")
        private String email;

        // Thông tin tài khoản quản lý
        @NotBlank(message = "Họ tên người quản lý không được để trống")
        @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
        private String fullName;

        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{4,30}$", message = "Tên đăng nhập chỉ được chứa chữ cái, số, dấu chấm, gạch dưới hoặc gạch ngang và có độ dài từ 4 đến 30 ký tự")
        private String userName;

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 50, message = "Mật khẩu phải có từ 8 đến 50 ký tự")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).+$", message = "Mật khẩu phải chứa ít nhất một chữ hoa, một chữ thường, một chữ số và một ký tự đặc biệt")
        private String password;

        @Pattern(regexp = "^(0|\\+84)[1-9][0-9]{8}$", message = "Số điện thoại người quản lý không hợp lệ")
        private String managerPhone;

        @NotBlank(message = "Email người quản lý không được để trống")
        @Email(message = "Email người quản lý không đúng định dạng")
        private String managerEmail;
}