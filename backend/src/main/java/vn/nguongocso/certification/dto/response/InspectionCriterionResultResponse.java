package vn.nguongocso.certification.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import vn.nguongocso.certification.enums.InspectionResultEntrySource;

/**
 * DTO phản hồi kết quả kiểm nghiệm cho một chỉ tiêu.
 */
@Getter
@Setter
@Builder
public class InspectionCriterionResultResponse {

    /**
     * ID duy nhất của kết quả kiểm nghiệm.
     */
    private String resultId;

    /**
     * ID của chỉ tiêu kiểm nghiệm.
     */
    private String criterionId;

    /**
     * ID chỉ tiêu trong danh mục dùng chung (catalog criterion ID).
     * Dùng để xác định identity của chỉ tiêu (không phải name).
     */
    private Long criterionDefinitionId;

    /**
     * Mã chỉ tiêu.
     */
    private String criterionCode;

    /**
     * Tên chỉ tiêu.
     */
    private String criterionName;

    /**
     * Ngày cấp kết quả kiểm nghiệm.
     */
    private LocalDate resultDate;

    /**
     * Ngày hết hiệu lực của kết quả.
     */
    private LocalDate expiryDate;

    /**
     * Kết quả kiểm nghiệm: true = đạt, false = không đạt.
     */
    private Boolean passed;

    /**
     * Đường dẫn tập tin phiếu kết quả.
     */
    private String filePath;

    /**
     * Tên người nhập kết quả.
     */
    private String createdByName;

    /**
     * Nguồn ghi nhận kết quả kiểm nghiệm (đơn vị kiểm nghiệm khai hoặc HTX tự nhập).
     */
    private InspectionResultEntrySource entrySource;

    /**
     * Thời gian tạo kết quả.
     */
    private LocalDateTime createdAt;

    /**
     * Thời gian cập nhật kết quả gần nhất.
     */
    private LocalDateTime updatedAt;
}
