package vn.nguongocso.report.enums;

/** Danh mục các loại cảnh báo trên lô sản xuất. */
public enum LotAlertType {
    RECALLING, // Lô đang hoặc đã trong diện thu hồi

    LOCKED_LABEL, // Mã tem bị khóa do nghi vấn giả mạo hoặc quét bất thường

    INSPECTION_FAILED, // Kiểm nghiệm không đạt chỉ tiêu an toàn/chất lượng

    QUARANTINE_OVERWRITTEN, // Sự kiện thu hoạch ghi đè trước thời gian cách ly

    SERIOUS_FEEDBACK_OPEN, // Phản ánh người tiêu dùng nghiêm trọng chưa đóng

    INSPECTION_EXPIRED; // Kết quả kiểm nghiệm đã hết hiệu lực

    /** Chuyển đổi an toàn từ chuỗi (hỗ trợ cả uppercase và snake_case). */
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
