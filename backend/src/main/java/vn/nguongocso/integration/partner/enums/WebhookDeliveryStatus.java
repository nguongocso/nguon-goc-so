package vn.nguongocso.integration.partner.enums;

/**
 * Trạng thái phân phối thông báo Webhook tới bên thứ ba (NCL-12-CN-006).
 */
public enum WebhookDeliveryStatus {
    /** Gửi thành công tới địa chỉ nhận của đối tác (HTTP 2xx). */
    SUCCESS,

    /** Gửi thất bại nhưng đang trong lịch chờ thử lại giãn dần. */
    PENDING_RETRY,

    /** Thất bại vĩnh viễn sau khi đã vượt quá số lần thử lại tối đa. */
    FAILED,

    /** Đã hủy gửi do khóa truy cập bị thu hồi (REVOKED - TC-04). */
    CANCELLED
}
