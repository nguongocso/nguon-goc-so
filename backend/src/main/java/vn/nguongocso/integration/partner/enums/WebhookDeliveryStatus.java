package vn.nguongocso.integration.partner.enums;

/**
 * Trạng thái phân phối thông báo Webhook tới bên thứ ba.
*/
public enum WebhookDeliveryStatus {
    SUCCESS,

    PENDING_RETRY,

    FAILED,

    CANCELLED
}
