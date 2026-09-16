package vn.nguongocso.organization.dto.request;

import java.util.UUID;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Yêu cầu cập nhật thông tin tổ chức.
 */
@Data
public class OrganizationUpdateRequest {
    @NotBlank(message = "Tên tổ chức không được để trống")
    private String name;

    private String address;

    /** ID đơn vị hành chính cấp tỉnh/thành phố. */
    private UUID provinceId;

    /** ID đơn vị hành chính cấp xã/phường. */
    private UUID communeId;

    @Pattern(regexp = "^(\\d{10,11})?$", message = "Số điện thoại phải có 10-11 chữ số")
    private String phone;

    @Email(message = "Email không hợp lệ")
    private String email;
}

