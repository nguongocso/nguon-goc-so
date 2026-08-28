package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO cho yêu cầu ghi nhận kết quả kiểm nghiệm một chỉ tiêu.
 *
 * <p>
 * Khi {@code passed = false} (Không đạt), {@code resultDate}, {@code expiryDate}
 * và {@code filePath} được phép là {@code null} vì chỉ tiêu không đạt không có
 * hiệu lực thời gian. Khi {@code passed = true} (Đạt), cả ba trường này
 * phải được cung cấp — validation được thực hiện ở tầng service.
 * </p>
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
     * Bắt buộc khi {@code passed = true}; được phép {@code null} khi {@code passed = false}.
     */
    private LocalDate resultDate;

    /**
     * Ngày hết hiệu lực của kết quả.
     * Bắt buộc khi {@code passed = true}; được phép {@code null} khi {@code passed = false}.
     */
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
