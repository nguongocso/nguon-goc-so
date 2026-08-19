package vn.nguongocso.trace.enums;

/**
 * Trạng thái của một lô hàng.
 */
public enum ShipmentStatus {
    DRAFT, // Mới tạo

    CODE_PRINTED, // Đã cấp/in mã

    ACTIVATED, // Đã kích hoạt tem

    RECALLED // Đã thu hồi
}
