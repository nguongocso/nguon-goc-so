package vn.nguongocso.auth.enums;

/** Mã nguyên nhân phát hiện bất thường đăng nhập. */
public enum AnomalyReasonCode {
    REPEATED_FAILED_LOGIN, // Sai mật khẩu liên tiếp ≥ 5 lần trong 2 phút
    UNUSUAL_COUNTRY       // Đăng nhập từ quốc gia chưa từng ghi nhận SUCCESS cho tài khoản
}
