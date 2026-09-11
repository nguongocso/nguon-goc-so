package vn.nguongocso.recall.enums;

/**
 * Trạng thái của một yêu cầu thu hồi hàng loạt theo phạm vi ảnh hưởng (NCL-08-CN-011).
 *
 * <ul>
 *   <li>{@code PENDING}: Yêu cầu đã được tạo, đang chờ quản lý xét duyệt.</li>
 *   <li>{@code APPROVED}: Yêu cầu đã được duyệt, các lô hàng trong phạm vi chuyển sang trạng thái RECALLING.</li>
 *   <li>{@code REJECTED}: Yêu cầu đã bị từ chối.</li>
 *   <li>{@code COMPLETED}: Vụ việc thu hồi đã hoàn thành xử lý, chuyển sang trạng thái Đã xử lý (NCL-08-CN-012).</li>
 * </ul>
 */
public enum BulkRecallRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    COMPLETED
}
