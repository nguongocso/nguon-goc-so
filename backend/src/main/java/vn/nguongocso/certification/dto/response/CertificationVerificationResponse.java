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
    private UUID id;

    private UUID organizationId;

    private String organizationName;

    private UUID standardId;

    private String standardName;

    private String code;

    private String issuedBy;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    private CertificationVerificationStatus verificationStatus;

    private CertificationValidityStatus validityStatus;

    private CertificateDocumentResponse document;

    private CertificateReviewerResponse reviewedBy;

    private LocalDateTime reviewedAt;

    private String reviewNote;

    private String rejectionReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer notifiedCount;
}
