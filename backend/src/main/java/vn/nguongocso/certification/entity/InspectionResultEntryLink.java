package vn.nguongocso.certification.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.certification.enums.InspectionResultEntryLinkStatus;
import vn.nguongocso.organization.entity.Organization;

/**
 * Liên kết có thời hạn và dùng một lần để đơn vị kiểm nghiệm nhập kết quả.
 */
@Entity
@Table(name = "inspection_result_entry_links")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionResultEntryLink {

    /**
     * Khóa chính định danh liên kết nhập kết quả.
     */
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    /**
     * Yêu cầu kiểm nghiệm tương ứng được cấp liên kết.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_request_id", nullable = false)
    private InspectionRequest inspectionRequest;

    /**
     * Hợp tác xã sở hữu yêu cầu kiểm nghiệm.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /**
     * Đơn vị kiểm nghiệm được chỉ định để nhập kết quả.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "testing_unit_id", nullable = false)
    private TestingUnit testingUnit;

    /**
     * Địa chỉ email người nhận đại diện đơn vị kiểm nghiệm.
     */
    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    /**
     * Tiền tố 8 ký tự đầu của token để hiển thị tham chiếu an toàn.
     */
    @Column(name = "token_prefix", nullable = false, length = 16)
    private String tokenPrefix;

    /**
     * Mã băm SHA-256 của token bí mật để xác thực.
     */
    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 64,
            columnDefinition = "CHAR(64)")
    private String tokenHash;

    /**
     * Trạng thái của liên kết nhập kết quả.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InspectionResultEntryLinkStatus status;

    /**
     * Thời điểm liên kết hết hiệu lực.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Thời điểm liên kết được sử dụng để nộp kết quả.
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * Địa chỉ IP của máy khách khi thực hiện nộp kết quả.
     */
    @Column(name = "used_ip", length = 45)
    private String usedIp;

    /**
     * Thông tin User-Agent của client khi thực hiện nộp kết quả.
     */
    @Column(name = "used_user_agent", length = 500)
    private String usedUserAgent;

    /**
     * Quản lý HTX đã tạo và cấp liên kết.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /**
     * Thời điểm tạo liên kết.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Quản lý HTX đã thu hồi liên kết này (nếu có).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private User revokedBy;

    /**
     * Thời điểm liên kết bị thu hồi (nếu có).
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = InspectionResultEntryLinkStatus.ACTIVE;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
