package vn.nguongocso.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request cập nhật thông tin hồ sơ cá nhân của người dùng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {

    @Pattern(
            regexp = "^(0[35789][0-9]{8})?$",
            message = "Số điện thoại không đúng định dạng (10 số, bắt đầu bằng 03, 05, 07, 08, 09)"
    )
    private String phone;

    @Email(message = "Địa chỉ email không hợp lệ")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;
}
