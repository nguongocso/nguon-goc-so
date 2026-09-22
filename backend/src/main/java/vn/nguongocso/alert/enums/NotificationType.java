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

    ACTIVITY_LOG_EXPORT_READY // Tệp nhật ký nền đã sẵn sàng
}
