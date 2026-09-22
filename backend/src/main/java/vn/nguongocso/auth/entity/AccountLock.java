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
import vn.nguongocso.auth.enums.AccountLockStatus;

/**
 * Ghi nhận vòng đời khoá/mở khoá tạm của một tài khoản.
 */
@Entity
@Table(name = "account_locks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLock {
    @Id
    @Column(name = "id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "anomaly_id", nullable = true)
    private LoginAnomaly anomaly;

    @ManyToOne
    @JoinColumn(name = "locked_by", nullable = false)
    private User lockedBy;

    @Column(nullable = true, length = 500)
    private String lockReason;

    @Column(nullable = false)
    private OffsetDateTime lockedAt;

    @Column(nullable = true)
    private OffsetDateTime lockUntil;

    @Column(nullable = false)
    private boolean permanent = false;

    @ManyToOne
    @JoinColumn(name = "unlocked_by", nullable = true)
    private User unlockedBy;

    @Column(nullable = true)
    private OffsetDateTime unlockedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountLockStatus status = AccountLockStatus.LOCKED;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (lockedAt == null) {
            lockedAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = AccountLockStatus.LOCKED;
        }
    }
}
