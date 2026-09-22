package vn.nguongocso.certification.enums;

/**
 * Trạng thái xác thực chứng nhận của tổ chức do Quản trị viên nền tảng (VT-01) xử lý.
 */
public enum CertificationVerificationStatus {
    /** Đang chờ Quản trị viên nền tảng kiểm tra. */
    PENDING,

    /** Đã xác thực thành công. */
    VERIFIED,

    /** Bị từ chối xác thực. */
    REJECTED
}
