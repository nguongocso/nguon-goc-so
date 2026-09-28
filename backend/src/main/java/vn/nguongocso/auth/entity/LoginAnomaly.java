package vn.nguongocso.auth.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.auth.enums.AnomalyReasonCode;
import vn.nguongocso.auth.enums.AnomalyStatus;
import vn.nguongocso.organization.entity.Organization;

/**
 * Ghi nhận các lần phát hiện bất thường đăng nhập của một tài khoản.
 */
@Entity
@Table(name = "login_anomalies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAnomaly {
    @Id
    @Column(name = "id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalyReasonCode reasonCode;

    @Column(nullable = true)
    private Integer attemptCount;

    @Column(nullable = false, length = 45)
    private String ipAddress;

    @Column(nullable = true, length = 2)
    private String countryCode;

    @Column(nullable = false)
    private OffsetDateTime detectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalyStatus status = AnomalyStatus.OPEN;

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID notificationId;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (detectedAt == null) {
            detectedAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = AnomalyStatus.OPEN;
        }
    }
}
