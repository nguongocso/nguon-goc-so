-- ============================================================
-- V20260908100000: Bảng nhắc việc ghi nhật ký theo mốc canh tác bắt buộc
--                  (User Story: NCL-03-CN-007)
-- ============================================================

CREATE TABLE milestone_reminders (
    id CHAR(36) NOT NULL,
    lot_id CHAR(36) NOT NULL,
    milestone_id BIGINT NOT NULL,
    user_id CHAR(36) NOT NULL,
    notification_id CHAR(36) NULL,
    overdue_days INT NOT NULL,
    reminder_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT pk_milestone_reminders PRIMARY KEY (id),
    CONSTRAINT fk_milestone_reminder_lot FOREIGN KEY (lot_id) REFERENCES production_lot (id),
    CONSTRAINT fk_milestone_reminder_milestone FOREIGN KEY (milestone_id) REFERENCES cultivation_milestone (id),
    CONSTRAINT fk_milestone_reminder_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_milestone_reminder_notification FOREIGN KEY (notification_id) REFERENCES notifications (id),
    CONSTRAINT uk_lot_milestone_date_user UNIQUE (lot_id, milestone_id, reminder_date, user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_milestone_reminder_lot_status ON milestone_reminders (lot_id, status);
CREATE INDEX idx_milestone_reminder_user_status ON milestone_reminders (user_id, status);
CREATE INDEX idx_milestone_reminder_date ON milestone_reminders (reminder_date);
