package vn.nguongocso.integration.partner.enums;

/** Trạng thái phân phối thông báo Webhook tới bên thứ ba. */
public enum WebhookDeliveryStatus {
    SUCCESS, // Thành công

    PENDING_RETRY, // Đang thử lại

    FAILED, // Thất bại

    CANCELLED // Đã hủy
}
