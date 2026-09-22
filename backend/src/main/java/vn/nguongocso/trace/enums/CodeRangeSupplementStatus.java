package vn.nguongocso.trace.enums;

/** Trạng thái yêu cầu cấp bổ sung dải mã truy xuất. */
public enum CodeRangeSupplementStatus {
    PENDING, // Đang chờ Quản trị viên nền tảng duyệt

    APPROVED, // Đã duyệt (toàn bộ hoặc một phần)

    REJECTED // Bị từ chối kèm lý do
}
