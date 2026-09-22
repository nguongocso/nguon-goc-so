package vn.nguongocso.farm.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.certification.entity.CultivationMilestone;
import vn.nguongocso.farm.enums.MilestoneReminderStatus;
import vn.nguongocso.notification.entity.Notification;

/**
 * Thực thể nhắc việc ghi nhật ký theo mốc canh tác bắt buộc (NCL-03-CN-007).
*/
@Entity
@Table(
    name = "milestone_reminders",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_lot_milestone_date_user",
            columnNames = {"lot_id", "milestone_id", "reminder_date", "user_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilestoneReminder {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private ProductionLot productionLot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "milestone_id", nullable = false)
    private CultivationMilestone milestone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id")
    private Notification notification;

    @Column(name = "overdue_days", nullable = false)
    private Integer overdueDays;

    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MilestoneReminderStatus status = MilestoneReminderStatus.OPEN;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Khởi tạo ID, thời điểm tạo và trạng thái mặc định trước khi lưu mới. */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.status == null) {
            this.status = MilestoneReminderStatus.OPEN;
        }
    }
    /** Cập nhật thời điểm sửa đổi trước khi lưu bản ghi hiện có. */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
