package vn.nguongocso.trace.enums;

/**
 * Trạng thái yêu cầu cấp bổ sung dải mã truy xuất (NCL-04-CN-007).
 */
public enum CodeRangeSupplementStatus {
    /** Đang chờ Quản trị viên nền tảng (VT-01) duyệt. */
    PENDING,
    /** Đã duyệt (toàn bộ hoặc một phần) — hạn mức đã tăng. */
    APPROVED,
    /** Bị từ chối kèm lý do. */
    REJECTED
}
