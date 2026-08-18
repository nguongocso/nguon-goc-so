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
 * 
 * <p>
 * Mỗi bản ghi đại diện cho một lần khoá (locked_at) và có thể một lần mở khoá sau
 * (unlocked_at).
 * Khoá là vô thời hạn cho đến khi được mở khoá thủ công.
 * </p>
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
    
    /**
     * Tài khoản bị khoá (bất kể vai trò VT-01..VT-05).
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Bản ghi bất thường dẫn tới khoá (nếu khoá do phát hiện bất thường).
     * Nullable nếu khoá do hành động thủ công của admin mà không liên kết với anomaly nào.
     */
    @ManyToOne
    @JoinColumn(name = "anomaly_id", nullable = true)
    private LoginAnomaly anomaly;
    
    /**
     * Người thực hiện khoá tạm (VT-01 hoặc quản lý tổ chức có quyền).
     */
    @ManyToOne
    @JoinColumn(name = "locked_by", nullable = false)
    private User lockedBy;
    
    /**
     * Ghi chú/lý do khoá tạm (tối đa 500 ký tự).
     */
    @Column(nullable = true, length = 500)
    private String lockReason;
    
    /**
     * Thời điểm khoá tạm.
     */
    @Column(nullable = false)
    private OffsetDateTime lockedAt;
    
    /**
     * Thời điểm hết hạn nếu khoá tạm. Null nghĩa là khoá vĩnh viễn.
     */
    @Column(nullable = true)
    private OffsetDateTime lockUntil;

    /**
     * True nếu đây là khóa vĩnh viễn, chỉ mở khóa thủ công bởi admin.
     */
    @Column(nullable = false)
    private boolean permanent = false;

    /**
     * Người thực hiện mở khoá (nếu đã mở khoá).
     */
    @ManyToOne
    @JoinColumn(name = "unlocked_by", nullable = true)
    private User unlockedBy;

    /**
     * Thời điểm mở khoá. Null nếu tài khoản vẫn còn khoá.
     */
    @Column(nullable = true)
    private OffsetDateTime unlockedAt;
    
    /**
     * Trạng thái hiện tại: LOCKED hoặc UNLOCKED.
     */
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
