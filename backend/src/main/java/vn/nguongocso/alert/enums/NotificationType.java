package vn.nguongocso.alert.enums;

/**
 * Loại thông báo.
 */
public enum NotificationType {
    TASK, // Nhiệm vụ

    ALERT, // Cảnh báo

    INFO, // Thông tin

    LOGIN_ANOMALY_DETECTED, // Phát hiện đăng nhập bất thường

    ACCOUNT_LOCKED, // Tài khoản bị khóa

    ACCOUNT_UNLOCKED, // Tài khoản được mở khóa

    ACTIVITY_LOG_EXPORT_READY, // Tệp nhật ký nền đã sẵn sàng

    FARM_LOG_SYNC_SUCCESS, // Đồng bộ nhật ký canh tác ngoại tuyến thành công

    FARM_LOG_SYNC_FAILED // Đồng bộ nhật ký canh tác ngoại tuyến thất bại
}
