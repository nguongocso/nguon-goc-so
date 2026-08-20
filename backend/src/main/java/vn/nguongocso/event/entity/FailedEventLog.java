package vn.nguongocso.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.event.enums.ChainEventType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity nhật ký sự kiện bị chặn (ghi lỗi).
 *
 * @author Triệu Văn Đại
 */
@Entity
@Table(name = "failed_event_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedEventLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private ChainEventType eventType;

    @Column(name = "lot_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID lotId;

    @Column(name = "lot_code")
    private String lotCode;

    @Column(name = "failure_reason", nullable = false, columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (attemptedAt == null) {
            attemptedAt = LocalDateTime.now();
        }
    }
}
