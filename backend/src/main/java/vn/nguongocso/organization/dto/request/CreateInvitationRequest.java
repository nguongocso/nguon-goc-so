package vn.nguongocso.organization.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Yêu cầu tạo lời mời tham gia tổ chức.
 */
@Getter
@Setter
public class CreateInvitationRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotNull(message = "Vai trò không được để trống")
    private Integer roleId;

    @Min(value = 1, message = "Thời hạn nhỏ nhất là 1 ngày")
    @Max(value = 30, message = "Thời hạn lớn nhất là 30 ngày")
    private Integer expiryDays = 7;
}
