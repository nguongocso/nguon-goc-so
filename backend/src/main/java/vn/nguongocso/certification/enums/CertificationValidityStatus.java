package vn.nguongocso.certification.enums;

/**
 * Trạng thái hiệu lực của chứng nhận tại thời điểm truy vấn (tính động dựa trên ngày hết hạn).
 *
 * <ul>
 *   <li>{@code VALID}: Ngày hết hạn &gt;= ngày hiện tại.</li>
 *   <li>{@code EXPIRED}: Ngày hết hạn &lt; ngày hiện tại.</li>
 * </ul>
 */
public enum CertificationValidityStatus {
    /** Còn hiệu lực thời gian. */
    VALID,

    /** Đã hết hạn hiệu lực. */
    EXPIRED
}
