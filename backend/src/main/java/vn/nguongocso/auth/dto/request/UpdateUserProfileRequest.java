package vn.nguongocso.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request cập nhật thông tin hồ sơ cá nhân của người dùng (NCL-01-CN-010).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {

    @Size(max = 255, message = "Họ và tên tối đa 255 ký tự")
    private String fullName;

    @Pattern(
            regexp = "^(0[35789][0-9]{8})?$",
            message = "Số điện thoại không đúng định dạng (10 số, bắt đầu bằng 03, 05, 07, 08, 09)"
    )
    private String phone;

    @Email(message = "Địa chỉ email không hợp lệ")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @Size(max = 500, message = "Đường dẫn ảnh đại diện tối đa 500 ký tự")
    private String avatarUrl;
}

