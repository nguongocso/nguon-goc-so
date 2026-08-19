package vn.nguongocso.integration.apikey.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.organization.entity.Organization;

/**
 * Thực thể lưu trữ khóa truy cập dành cho đối tác bên thứ ba (NCL-12-CN-001).
 * <p>
 * Lưu băm SHA-256 của khóa và tích hợp các chỉ số thống kê đếm lượt gọi.
 */
@Entity
@Table(name = "partner_api_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "partner_name", nullable = false)
    private String partnerName;

    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @Column(name = "rate_limit_per_hour", nullable = false)
    private Integer rateLimitPerHour;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PartnerApiKeyStatus status;

    @Builder.Default
    @Column(name = "total_calls", nullable = false)
    private Long totalCalls = 0L;

    @Builder.Default
    @Column(name = "failed_calls", nullable = false)
    private Long failedCalls = 0L;

    @Column(name = "last_called_at")
    private LocalDateTime lastCalledAt;

    @Column(name = "last_call_status")
    private Integer lastCallStatus;

    @Column(name = "last_call_ip", length = 45)
    private String lastCallIp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private User revokedBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = PartnerApiKeyStatus.ACTIVE;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (totalCalls == null) {
            totalCalls = 0L;
        }
        if (failedCalls == null) {
            failedCalls = 0L;
        }
    }
}
