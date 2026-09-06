package vn.nguongocso.certification.dto.response;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.certification.enums.InspectionBlockReasonCode;

/**
 * Kết quả đánh giá điều kiện kiểm nghiệm của lô sản xuất theo QTN-30
 * (NCL-11-CN-005): lô chưa đạt kiểm nghiệm không được tạo lô hàng.
 *
 * <p>
 * Dùng làm nguồn sự thật chung cho:
 * <ul>
 * <li>Gate tại {@code POST /api/v1/shipments} (chặn tạo lô hàng — TC-01)</li>
 * <li>Gate tại {@code POST /api/v1/shipments/{id}/activate}
 * (rào chắn thứ hai trước khi kích hoạt tem — QTN-21)</li>
 * </ul>
 * </p>
 */
@Getter
@Setter
@Builder
public class InspectionEligibilityResult {

    /**
     * Lô có đủ điều kiện tạo lô hàng / kích hoạt tem hay không.
     */
    private boolean eligible;

    /**
     * Mã nguyên nhân bị chặn — null khi {@link #eligible} = true.
     */
    private InspectionBlockReasonCode reasonCode;

    /**
     * Thông điệp lỗi tiếng Việt khi bị chặn — null khi đủ điều kiện.
     */
    private String message;

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

    /**
     * Ngày hết hiệu lực sớm nhất trong các kết quả đạt (nếu có).
     */
    private LocalDate earliestExpiryDate;
}
