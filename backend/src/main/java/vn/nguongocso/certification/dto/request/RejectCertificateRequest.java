package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu từ chối xác thực chứng nhận của Quản trị viên nền tảng (VT-01).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectCertificateRequest {

    /**
     * Lý do từ chối chứng nhận (bắt buộc, tối đa 1000 ký tự).
     */
    @NotBlank(message = "Lý do từ chối không được để trống")
    @Size(max = 1000, message = "Lý do từ chối không được vượt quá 1000 ký tự")
    private String rejectionReason;
}
