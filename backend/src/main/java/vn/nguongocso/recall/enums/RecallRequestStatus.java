package vn.nguongocso.recall.enums;

/**
 * Trạng thái của một yêu cầu thu hồi lô sản xuất (NCL-08-CN-008).
 *
 * <ul>
 *   <li>{@code PENDING}: Yêu cầu đã được tạo, đang chờ quản lý xét duyệt.</li>
 *   <li>{@code APPROVED}: Yêu cầu đã được duyệt, lô sản xuất chuyển sang trạng thái RECALLED.</li>
 *   <li>{@code REJECTED}: Yêu cầu đã bị từ chối.</li>
 * </ul>
 */
public enum RecallRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}