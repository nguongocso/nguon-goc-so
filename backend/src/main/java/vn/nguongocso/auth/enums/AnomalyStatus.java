package vn.nguongocso.auth.enums;

/** Trạng thái của bản ghi bất thường đăng nhập. */
public enum AnomalyStatus {
    OPEN,      // Bất thường mới phát hiện, chưa giải quyết
    DISMISSED  // Bất thường đã được giải quyết/loại bỏ
}
