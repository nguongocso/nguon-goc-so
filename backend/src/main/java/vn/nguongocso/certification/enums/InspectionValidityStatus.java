package vn.nguongocso.certification.enums;

/**
 * Trạng thái hiệu lực kết quả kiểm nghiệm của lô sản xuất (NCL-11-CN-004).
 */
public enum InspectionValidityStatus {
    NOT_REQUIRED, // Lô thuộc loại nông sản không bắt buộc kiểm nghiệm.

    NO_VALID_RESULT, // Lô bắt buộc kiểm nghiệm nhưng chưa có kết quả kiểm nghiệm đạt hợp lệ

    VALID, // Kết quả kiểm nghiệm đạt, còn hiệu lực và số ngày còn lại lớn hơn ngưỡng cảnh
           // báo.

    EXPIRING, // Kết quả kiểm nghiệm đạt, còn hiệu lực nhưng số ngày còn lại nằm trong ngưỡng

    EXPIRED // Kết quả kiểm nghiệm đã quá ngày hết hiệu lực (hết hạn).
}
