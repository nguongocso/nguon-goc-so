package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO yêu cầu cấp liên kết nhập kết quả kiểm nghiệm cho đơn vị kiểm nghiệm.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueInspectionResultEntryLinkRequest {

    /**
     * Địa chỉ email người nhận tại đơn vị kiểm nghiệm.
     */
    @NotBlank(message = "Email người nhận không được để trống")
    @Email(message = "Email người nhận không hợp lệ")
    private String recipientEmail;

    /**
     * Thời hạn hiệu lực của liên kết (ngày), từ 1 đến 30 ngày, mặc định 7 ngày.
     */
    @Min(value = 1, message = "Thời hạn liên kết tối thiểu là 1 ngày")
    @Max(value = 30, message = "Thời hạn liên kết tối đa là 30 ngày")
    @Builder.Default
    private Integer expiryDays = 7;
}
