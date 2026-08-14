-- ============================================================
-- V8: Alerts and notifications
-- ============================================================

CREATE TABLE alerts (
    id CHAR(36) NOT NULL,
    type VARCHAR(50) NOT NULL,
    related_entity_type VARCHAR(100) NOT NULL,
    related_entity_id CHAR(36) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    details JSON NOT NULL,
    status VARCHAR(20) NOT NULL,
    message TEXT,
    organization_id CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL,
    resolved_at DATETIME NULL,
    resolved_by CHAR(36) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_alert_org FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    INDEX idx_alert_type (type),
    INDEX idx_alert_status (status),
    INDEX idx_alert_created_at (created_at),
    INDEX idx_alert_related_entity (related_entity_id)
) ENGINE=InnoDB;

CREATE TABLE notifications (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_notification_user (user_id),
    INDEX idx_notification_read (is_read),
    INDEX idx_notification_created (created_at)
) ENGINE=InnoDB;