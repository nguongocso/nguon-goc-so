package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu xác thực chứng nhận của Quản trị viên nền tảng (VT-01).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyCertificateRequest {

    /**
     * Ghi chú xác thực (tùy chọn, tối đa 1000 ký tự).
     */
    @Size(max = 1000, message = "Ghi chú xác thực không được vượt quá 1000 ký tự")
    private String reviewNote;
}
