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
 * Entity nhật ký đồng bộ các sự kiện ngoại tuyến.
 */
@Entity
@Table(name = "offline_sync_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineSyncLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "sync_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID syncId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "offline_event_id", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID offlineEventId;

    @Column(name = "production_lot_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID productionLotId;

    @Column(name = "shipment_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID shipmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private ChainEventType eventType;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // SUCCESS, DUPLICATE, FAILED

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "synced_at", nullable = false, updatable = false)
    private LocalDateTime syncedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (syncedAt == null) {
            syncedAt = LocalDateTime.now();
        }
    }
}
