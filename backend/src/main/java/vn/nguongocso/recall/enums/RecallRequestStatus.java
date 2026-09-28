package vn.nguongocso.recall.enums;

/** Trạng thái của một yêu cầu thu hồi lô sản xuất. */
public enum RecallRequestStatus {
    PENDING, // Chờ xét duyệt

    APPROVED, // Đã duyệt

    REJECTED // Đã từ chối
}
