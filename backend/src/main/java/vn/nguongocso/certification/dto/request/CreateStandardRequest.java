package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Lớp CreateStandardRequest đại diện cho yêu cầu tạo tiêu chuẩn trong hệ thống.
 */
@Getter
@Setter
public class CreateStandardRequest {
    @NotBlank(message = "Tên tiêu chuẩn không được để trống.")
    @Size(max = 255, message = "Tên tiêu chuẩn không được vượt quá 255 ký tự.")
    private String name;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự.")
    private String description;

    @Size(max = 255, message = "Cơ quan ban hành không được vượt quá 255 ký tự.")
    private String issuingBody;
}