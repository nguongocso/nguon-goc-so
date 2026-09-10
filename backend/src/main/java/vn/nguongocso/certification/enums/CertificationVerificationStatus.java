package vn.nguongocso.certification.enums;

/**
 * Trạng thái xác thực chứng nhận của tổ chức do Quản trị viên nền tảng (VT-01) xử lý.
 *
 * <ul>
 *   <li>{@code PENDING}: Đang chờ Quản trị viên nền tảng kiểm tra, đối chiếu.</li>
 *   <li>{@code VERIFIED}: Đã xác thực tính chân thực và hợp lệ.</li>
 *   <li>{@code REJECTED}: Bị từ chối do không khớp hoặc vi phạm quy định.</li>
 * </ul>
 */
public enum CertificationVerificationStatus {
    /** Đang chờ Quản trị viên nền tảng kiểm tra. */
    PENDING,

    /** Đã xác thực thành công. */
    VERIFIED,

    /** Bị từ chối xác thực. */
    REJECTED
}
