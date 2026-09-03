package vn.nguongocso.publicapi.dto.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO chi tiết kết quả kiểm nghiệm của từng chỉ tiêu trên cổng tra cứu công khai.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicInspectionCriterionResultDto {

    /**
     * ID của kết quả kiểm nghiệm.
     */
    private String id;

    /**
     * Tên chỉ tiêu kiểm nghiệm.
     */
    private String criterionName;

    /**
     * Ngưỡng quy định / Tiêu chuẩn áp dụng.
     */
    private String standardValue;

    /**
     * Giá trị đo được / kết quả đánh giá thực tế.
     */
    private String measuredValue;

    /**
     * Kết luận đạt chuẩn hay không đạt.
     */
    private Boolean passed;

    /**
     * Tên chuyên viên kiểm nghiệm (nếu có).
     */
    private String inspectorName;

    /**
     * Ngày kiểm nghiệm / cấp kết quả.
     */
    private LocalDate inspectionDate;

    /**
     * Ngày hết hiệu lực của kết quả.
     */
    private LocalDate expiryDate;

    /**
     * Đơn vị / phòng kiểm nghiệm.
     */
    private String laboratoryName;
}
