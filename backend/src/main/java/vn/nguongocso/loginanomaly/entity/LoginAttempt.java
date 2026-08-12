package vn.nguongocso.loginanomaly.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lần đăng nhập thất bại được ghi nhận để phục vụ phát hiện
 * đăng nhập bất thường (ví dụ: sai mật khẩu nhiều lần trong
 * một khoảng thời gian ngắn).
 */
@Entity
@Table(name = "login_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    @Id
    @Column(name = "attempt_id", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID attemptId;

    /** Tên đăng nhập của lần thất bại. */
    @Column(nullable = false, length = 100)
    private String username;

    /** ID người dùng (nếu tài khoản tồn tại), ngược lại là null. */
    @Column(name = "user_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    /** IP của client thực hiện lần đăng nhập thất bại. */
    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    /** Lý do thất bại (ví dụ: "Sai mật khẩu"). */
    @Column(length = 255)
    private String reason;

    /** Thời điểm thất bại. */
    @Column(name = "failed_at", nullable = false)
    private LocalDateTime failedAt;

    @PrePersist
    public void prePersist() {
        if (attemptId == null) {
            attemptId = UUID.randomUUID();
        }
        if (failedAt == null) {
            failedAt = LocalDateTime.now();
        }
    }
}
