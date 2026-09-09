package vn.nguongocso.publicapi.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.certification.enums.CertificationStatus;
import vn.nguongocso.certification.enums.CertificationValidityStatus;
import vn.nguongocso.certification.enums.CertificationVerificationStatus;

/**
 * Response thông tin chứng nhận hiển thị trên trang tra cứu công khai.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicCertificationResponse {
    /** ID chứng nhận. */
    private UUID certificationId;

    /** Tên chứng nhận. */
    private String certificationName;

    /** Số hiệu chứng nhận. */
    private String certificationCode;

    /** Cơ quan/đơn vị cấp chứng nhận. */
    private String issuedBy;

    /** Ngày cấp. */
    private LocalDate issueDate;

    /** Ngày hết hạn. */
    private LocalDate expiryDate;

    /** Trạng thái xác thực (PENDING, VERIFIED). */
    private CertificationVerificationStatus verificationStatus;

    /** Trạng thái hiệu lực theo ngày (VALID, EXPIRED). */
    private CertificationValidityStatus validityStatus;

    /** Trạng thái hiển thị công khai (PENDING_VERIFICATION, VERIFIED, EXPIRED). */
    private String publicStatus;

    /** Trạng thái hiệu lực cũ (để tương thích ngược). */
    private CertificationStatus status;

    /** Nhãn hiển thị cho người tiêu dùng (Đang chờ xác thực, Đã đạt chuẩn, Đã hết hạn). */
    private String statusLabel;
}