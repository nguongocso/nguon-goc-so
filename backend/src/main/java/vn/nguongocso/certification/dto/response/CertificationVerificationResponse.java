package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.certification.enums.CertificationValidityStatus;
import vn.nguongocso.certification.enums.CertificationVerificationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thông tin chi tiết chứng nhận phục vụ xác thực dành cho Quản trị viên nền tảng (VT-01).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CertificationVerificationResponse {

    /** ID chứng nhận. */
    private UUID id;

    /** ID tổ chức sở hữu chứng nhận. */
    private UUID organizationId;

    /** Tên tổ chức sở hữu chứng nhận. */
    private String organizationName;

    /** ID tiêu chuẩn chứng nhận. */
    private UUID standardId;

    /** Tên tiêu chuẩn chứng nhận. */
    private String standardName;

    /** Số hiệu chứng nhận. */
    private String code;

    /** Cơ quan/đơn vị cấp chứng nhận. */
    private String issuedBy;

    /** Ngày cấp. */
    private LocalDate issueDate;

    /** Ngày hết hạn. */
    private LocalDate expiryDate;

    /** Trạng thái xác thực (PENDING, VERIFIED, REJECTED). */
    private CertificationVerificationStatus verificationStatus;

    /** Trạng thái hiệu lực theo thời gian (VALID, EXPIRED). */
    private CertificationValidityStatus validityStatus;

    /** Metadata tệp đính kèm phục vụ đối chiếu. */
    private CertificateDocumentResponse document;

    /** Quản trị viên đã ra quyết định duyệt/từ chối. */
    private CertificateReviewerResponse reviewedBy;

    /** Thời điểm ra quyết định. */
    private LocalDateTime reviewedAt;

    /** Ghi chú xác thực của Quản trị viên. */
    private String reviewNote;

    /** Lý do từ chối của Quản trị viên. */
    private String rejectionReason;

    /** Thời điểm tổ chức nộp chứng nhận lên hệ thống. */
    private LocalDateTime createdAt;

    /** Thời điểm cập nhật cuối cùng. */
    private LocalDateTime updatedAt;

    /** Số lượng thông báo đã gửi thành công tới HTX (khi từ chối). */
    private Integer notifiedCount;
}
