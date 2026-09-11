package vn.nguongocso.certification.enums;

/**
 * Trạng thái hiệu lực kết quả kiểm nghiệm của lô sản xuất (NCL-11-CN-004).
 *
 * <p>
 * Đây là giá trị suy diễn tại thời điểm đọc (derived state),
 * không lưu cứng vào cột database của lô sản xuất.
 * </p>
 */
public enum InspectionValidityStatus {

    /**
     * Lô thuộc loại nông sản không bắt buộc kiểm nghiệm.
     */
    NOT_REQUIRED,

    /**
     * Lô bắt buộc kiểm nghiệm nhưng chưa có kết quả kiểm nghiệm đạt hợp lệ
     * (chưa kiểm nghiệm, đang chờ kết quả, hoặc chưa đạt đủ mọi chỉ tiêu).
     */
    NO_VALID_RESULT,

    /**
     * Kết quả kiểm nghiệm đạt, còn hiệu lực và số ngày còn lại lớn hơn ngưỡng cảnh báo.
     */
    VALID,

    /**
     * Kết quả kiểm nghiệm đạt, còn hiệu lực nhưng số ngày còn lại nằm trong ngưỡng cảnh báo.
     */
    EXPIRING,

    /**
     * Kết quả kiểm nghiệm đã quá ngày hết hiệu lực (hết hạn).
     */
    EXPIRED
}
