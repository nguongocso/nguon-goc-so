-- ============================================================
-- V10: Report access logs, activity logs, scan logs, failed event logs
-- ============================================================

CREATE TABLE report_access_log (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    target_organization_id CHAR(36) NOT NULL,
    report_name VARCHAR(255) NOT NULL,
    accessed_at DATETIME NOT NULL,
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_report_access_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_report_access_org FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    CONSTRAINT fk_report_access_target_org FOREIGN KEY (target_organization_id) REFERENCES organizations(organization_id)
) ENGINE=InnoDB;

-- activity_logs: matches ActivityLog entity exactly
CREATE TABLE activity_logs (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    username VARCHAR(100) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    entity_type VARCHAR(50),
    entity_id CHAR(36),
    ip_address VARCHAR(45),
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_activity_log_user (user_id),
    INDEX idx_activity_log_created (created_at),
    INDEX idx_activity_log_org (organization_id),
    INDEX idx_activity_log_action (action)
) ENGINE=InnoDB;

-- trace_code_scan_logs: matches TraceCodeScanLog entity
CREATE TABLE trace_code_scan_logs (
    id CHAR(36) NOT NULL,
    trace_code_id CHAR(36) NOT NULL,
    scanned_at DATETIME NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    location VARCHAR(255),
    is_abnormal BOOLEAN NOT NULL DEFAULT FALSE,
    abnormal_reason VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_scan_log_trace_code FOREIGN KEY (trace_code_id) REFERENCES trace_codes(id),
    INDEX idx_scan_log_trace_code (trace_code_id),
    INDEX idx_scan_log_scanned_at (scanned_at)
) ENGINE=InnoDB;

-- failed_event_logs: matches FailedEventLog entity exactly
CREATE TABLE failed_event_logs (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    lot_id CHAR(36) NOT NULL,
    lot_code VARCHAR(255),
    failure_reason TEXT NOT NULL,
    attempted_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_failed_event_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;