package vn.nguongocso.auth.enums;

/** Trạng thái của bản ghi bất thường đăng nhập. */
public enum AnomalyStatus {
    OPEN,           // Bất thường mới phát hiện, chưa xử lý
    ACCOUNT_LOCKED, // Tài khoản đã bị khóa tạm do bất thường này
    DISMISSED       // Bất thường đã được giải quyết/loại bỏ
}
