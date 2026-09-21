package vn.nguongocso.certification.enums;

/**
 * Trạng thái hiệu lực của chứng nhận tại thời điểm truy vấn (tính động dựa trên
 * ngày hết hạn).
 */
public enum CertificationValidityStatus {
    /** Còn hiệu lực thời gian. */
    VALID,

    /** Đã hết hạn hiệu lực. */
    EXPIRED
}
