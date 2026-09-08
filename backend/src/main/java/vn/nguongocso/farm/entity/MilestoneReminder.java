package vn.nguongocso.farm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.certification.entity.CultivationMilestone;
import vn.nguongocso.farm.enums.MilestoneReminderStatus;
import vn.nguongocso.notification.entity.Notification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thực thể nhắc việc ghi nhật ký theo mốc canh tác bắt buộc (NCL-03-CN-007).
 *
 * <p>Được tạo tự động khi hệ thống quét phát hiện một mốc canh tác bắt buộc của
 * lô sản xuất chưa được ghi nhật ký và đã quá ngày dự kiến từ ngày gieo trồng.</p>
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

    /** Lô sản xuất liên quan. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private ProductionLot productionLot;

    /** Mốc canh tác bắt buộc bị thiếu/quá hạn. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "milestone_id", nullable = false)
    private CultivationMilestone milestone;

    /** Người dùng nhận nhắc việc (người được phân công hoặc tạo lô). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Thông báo tương ứng được tạo qua cơ chế NCL-08-CN-005. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id")
    private Notification notification;

    /** Số ngày quá hạn tại thời điểm tạo nhắc việc. */
    @Column(name = "overdue_days", nullable = false)
    private Integer overdueDays;

    /** Ngày phát nhắc việc (phục vụ chống tạo trùng trong cùng ngày). */
    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    /** Trạng thái nhắc việc (OPEN hoặc COMPLETED). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MilestoneReminderStatus status = MilestoneReminderStatus.OPEN;

    /** Thời điểm tự đóng khi đã ghi nhật ký cho mốc. */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
