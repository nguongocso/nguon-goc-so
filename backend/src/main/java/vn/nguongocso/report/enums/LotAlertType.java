package vn.nguongocso.report.enums;

/**
 * Danh mục các loại cảnh báo trên lô sản xuất phục vụ Cán bộ quản lý ngành (NCL-07-CN-006).
 */
public enum LotAlertType {
    /**
     * Lô đang hoặc đã trong diện thu hồi (QTN-09).
     */
    RECALLING,

    /**
     * Lô có ít nhất một mã tem bị khóa do nghi vấn giả mạo hoặc quét bất thường (QTN-10).
     */
    LOCKED_LABEL,

    /**
     * Lô có kết quả kiểm nghiệm không đạt chỉ tiêu an toàn/chất lượng (QTN-30).
     */
    INSPECTION_FAILED,

    /**
     * Lô có sự kiện thu hoạch ghi đè trước thời gian cách ly PHI an toàn (NCL-03-CN-005).
     */
    QUARANTINE_OVERWRITTEN,

    /**
     * Lô có phản ánh người tiêu dùng ở mức nghiêm trọng chưa đóng (NCL-08-CN-009).
     */
    SERIOUS_FEEDBACK_OPEN,

    /**
     * Lô có kết quả kiểm nghiệm đã hết hiệu lực (NCL-11-CN-004, QTN-13, QTN-21).
     */
    INSPECTION_EXPIRED;

    /**
     * Chuyển đổi an toàn từ chuỗi (hỗ trợ cả uppercase và snake_case như inspection_failed).
     *
     * @param value giá trị chuỗi đầu vào
     * @return LotAlertType tương ứng hoặc null nếu không khớp
     */
    public static LotAlertType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        for (LotAlertType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
