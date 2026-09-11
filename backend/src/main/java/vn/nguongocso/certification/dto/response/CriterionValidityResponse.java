package vn.nguongocso.certification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.certification.enums.InspectionValidityStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO chi tiết hiệu lực của từng tiêu chí kiểm nghiệm (NCL-11-CN-004).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriterionValidityResponse {

    /**
     * ID của chỉ tiêu kiểm nghiệm.
     */
    private Long criterionId;

    /**
     * Mã chỉ tiêu kiểm nghiệm (nếu có).
     */
    private String criterionCode;

    /**
     * Tên chỉ tiêu kiểm nghiệm (tiếng Việt).
     */
    private String criterionName;

    /**
     * Kết quả có Đạt hay không.
     */
    private Boolean passed;

    /**
     * Ngày hết hiệu lực của kết quả chỉ tiêu này.
     */
    private LocalDate expiryDate;

    /**
     * Số ngày còn lại (khi còn hạn).
     */
    private Long daysRemaining;

    /**
     * Số ngày quá hạn (khi đã hết hạn).
     */
    private Long daysOverdue;

    /**
     * Trạng thái hiệu lực của riêng chỉ tiêu này.
     */
    private InspectionValidityStatus status;
}
