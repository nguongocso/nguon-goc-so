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
    @NotNull(message = "ID chỉ tiêu kiểm nghiệm không được để trống.")
    private String criterionId;

    private LocalDate resultDate;

    private LocalDate expiryDate;

    @NotNull(message = "Kết quả kiểm nghiệm không được để trống.")
    private Boolean passed;

    private String filePath;
}
