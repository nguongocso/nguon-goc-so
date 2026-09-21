package vn.nguongocso.certification.dto.response;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.certification.enums.InspectionBlockReasonCode;

/**
 * Kết quả đánh giá điều kiện kiểm nghiệm của lô sản xuất theo QTN-30 (NCL-11-CN-005): lô chưa đạt kiểm nghiệm không
 * được tạo lô hàng.
 */
@Getter
@Setter
@Builder
public class InspectionEligibilityResult {
    private boolean eligible;

    private InspectionBlockReasonCode reasonCode;

    private String message;

    private Integer totalCriteria;

    private Integer passedCriteria;

    private Integer failedOrExpiredCriteria;

    private LocalDate earliestExpiryDate;
}
