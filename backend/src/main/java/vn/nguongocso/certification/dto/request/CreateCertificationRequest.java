package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO cho yêu cầu tạo chứng nhận mới.
 */
@Getter
@Setter
public class CreateCertificationRequest {
    @NotNull(message = "ID tiêu chuẩn không được để trống")
    private UUID standardId;

    @NotBlank(message = "Số hiệu chứng nhận không được để trống")
    @Size(max = 50, message = "Số hiệu chứng nhận tối đa 50 ký tự")
    private String code;

    @Size(max = 255, message = "Cơ quan cấp tối đa 255 ký tự")
    private String issuedBy;

    @NotNull(message = "Ngày cấp không được để trống")
    private LocalDate issueDate;

    @NotNull(message = "Ngày hết hạn không được để trống")
    private LocalDate expiryDate;
}