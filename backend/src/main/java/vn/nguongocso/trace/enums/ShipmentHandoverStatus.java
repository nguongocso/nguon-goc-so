package vn.nguongocso.trace.enums;

/**
 * Trạng thái phiếu bàn giao lô hàng.
 * Transition chỉ từ PENDING_CONFIRMATION sang 1 trong 4 trạng thái còn lại.
 */
public enum ShipmentHandoverStatus {
    PENDING_CONFIRMATION,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    CANCELLED
}
