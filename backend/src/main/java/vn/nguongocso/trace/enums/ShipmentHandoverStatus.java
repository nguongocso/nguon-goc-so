package vn.nguongocso.trace.enums;

/** Trạng thái phiếu bàn giao lô hàng. */
public enum ShipmentHandoverStatus {
    PENDING_CONFIRMATION, // Chờ xác nhận

    ACCEPTED, // Đã tiếp nhận

    REJECTED, // Đã từ chối

    EXPIRED, // Hết hạn

    CANCELLED // Đã hủy
}
