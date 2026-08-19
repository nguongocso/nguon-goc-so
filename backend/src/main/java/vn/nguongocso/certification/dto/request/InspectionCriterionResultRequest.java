package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO cho yêu cầu ghi nhận kết quả kiểm nghiệm một chỉ tiêu.
 */
@Getter
@Setter
@Builder
public class InspectionCriterionResultRequest {

    /**
     * ID của chỉ tiêu kiểm nghiệm cần ghi nhận kết quả.
     */
    @NotNull(message = "ID chỉ tiêu kiểm nghiệm không được để trống.")
    private String criterionId;

    /**
     * Ngày cấp kết quả kiểm nghiệm.
     */
    @NotNull(message = "Ngày cấp không được để trống.")
    private LocalDate resultDate;

    /**
     * Ngày hết hiệu lực của kết quả.
     */
    @NotNull(message = "Ngày hết hiệu lực không được để trống.")
    private LocalDate expiryDate;

    /**
     * Kết quả kiểm nghiệm: true = đạt, false = không đạt.
     */
    @NotNull(message = "Kết quả kiểm nghiệm không được để trống.")
    private Boolean passed;

    /**
     * Đường dẫn tập tin phiếu kết quả (tùy chọn).
     */
    private String filePath;
}
