package vn.nguongocso.certification.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO phản hồi để kiểm tra điều kiện kích hoạt tem.
 */
@Getter
@Setter
@Builder
public class CanActivateSealCheckResponse {

    /**
     * ID của lô sản xuất.
     */
    private String productionLotId;

    /**
     * Có thể kích hoạt tem hay không.
     */
    private Boolean canActivate;

    /**
     * Lý do không thể kích hoạt (nếu có).
     */
    private String reason;

    /**
     * Ngày hết hiệu lực sớm nhất của kết quả kiểm nghiệm.
     */
    private LocalDate earliestExpiryDate;

    /**
     * Tổng số chỉ tiêu bắt buộc đang được gán (ACTIVE) cho loại nông
     * sản của lô — cùng nguồn với GET test-criteria.
     */
    private Integer totalCriteria;

    /**
     * Số chỉ tiêu có kết quả MỚI NHẤT đạt và còn hiệu lực.
     */
    private Integer passedCriteria;

    /**
     * Số chỉ tiêu chưa có kết quả / không đạt / đã quá hạn.
     */
    private Integer failedOrExpiredCriteria;
}
