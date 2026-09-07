package vn.nguongocso.certification.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Một mục trong lịch sử kiểm nghiệu của một chỉ tiêu — đại diện cho
 * kết quả kiểm nghiệm tại một thời điểm (một yêu cầu kiểm nghiệm).
 *
 * <p>
 * Dùng trong {@link CriterionHistoryResponse} để trình bày dòng thời gian
 * kết quả của chỉ tiêu qua nhiều lần kiểm nghiệm / kiểm nghiệm lại.
 * </p>
 */
@Getter
@Setter
@Builder
public class CriterionHistoryEntry {

    /**
     * ID yêu cầu kiểm nghiệm chứa kết quả này.
     */
    private String requestId;

    /**
     * Ngày gửi mẫu của yêu cầu kiểm nghiệm.
     */
    private LocalDate sampleSentDate;

    /**
     * Ngày cấp kết quả (result date).
     */
    private LocalDate resultDate;

    /**
     * Ngày hết hiệu lực kết quả.
     */
    private LocalDate expiryDate;

    /**
     * Kết quả đạt / không đạt.
     */
    private boolean passed;

    /**
     * Tên đơn vị kiểm nghiệm.
     */
    private String testingUnit;

    /**
     * Tên người nhập kết quả.
     */
    private String createdByName;

    /**
     * Thời gian ghi nhận kết quả.
     */
    private LocalDateTime createdAt;
}
