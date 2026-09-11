package vn.nguongocso.trace.enums;

/**
 * Trạng thái của một lô hàng.
 */
public enum ShipmentStatus {
    DRAFT, // Mới tạo

    CODE_PRINTED, // Đã cấp/in mã

    ACTIVATED, // Đã kích hoạt tem

    SPLIT, // Đã tách thành các lô con

    RECALLING, // Đang thu hồi

    RECALLED // Đã thu hồi
}
