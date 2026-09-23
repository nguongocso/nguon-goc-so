package vn.nguongocso.recall.enums;

/** Trạng thái của một yêu cầu thu hồi hàng loạt theo phạm vi ảnh hưởng. */
public enum BulkRecallRequestStatus {
    PENDING, // Chờ xét duyệt

    APPROVED, // Đã duyệt

    REJECTED, // Đã từ chối

    COMPLETED // Vụ việc thu hồi đã hoàn thành xử lý
}
