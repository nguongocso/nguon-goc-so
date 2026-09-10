package vn.nguongocso.certification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.certification.enums.CertificationVerificationStatus;
import vn.nguongocso.organization.entity.Organization;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp Certification đại diện cho chứng nhận trong hệ thống.
 */
@Entity
@Table(name = "certifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certification {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false)
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_id", nullable = false)
    private Standard standard;
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "issued_by")
    private String issuedBy;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    /** Trạng thái xác thực của chứng nhận (mặc định PENDING). */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private CertificationVerificationStatus verificationStatus = CertificationVerificationStatus.PENDING;

    /** Người dùng (Quản trị viên VT-01) đã duyệt hoặc từ chối chứng nhận. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    /** Thời điểm phê duyệt hoặc từ chối chứng nhận. */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** Ghi chú của quản trị viên khi xác thực chứng nhận. */
    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    /** Lý do từ chối chứng nhận của quản trị viên (bắt buộc khi từ chối). */
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    /** Tên gốc của tệp chứng nhận khi tải lên. */
    @Column(name = "document_file_name")
    private String documentFileName;

    /** Loại MIME của tệp (application/pdf, image/jpeg, image/png). */
    @Column(name = "document_content_type", length = 100)
    private String documentContentType;

    /** Dung lượng tệp đính kèm (bytes). */
    @Column(name = "document_file_size")
    private Long documentFileSize;

    /** Đường dẫn lưu trữ tệp trên máy chủ (riêng tư, không để lộ trực tiếp). */
    @Column(name = "document_storage_path", length = 500)
    private String documentStoragePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (verificationStatus == null) {
            verificationStatus = CertificationVerificationStatus.PENDING;
        }
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}