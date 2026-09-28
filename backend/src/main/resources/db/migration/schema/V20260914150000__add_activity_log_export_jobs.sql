ALTER TABLE activity_logs
    ADD COLUMN actor_role VARCHAR(50) NULL AFTER full_name,
    ADD COLUMN before_value TEXT NULL AFTER entity_id,
    ADD COLUMN after_value TEXT NULL AFTER before_value;

CREATE TABLE activity_log_export_jobs (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    requested_by CHAR(36) NOT NULL,
    requested_by_username VARCHAR(100) NOT NULL,
    requested_by_role VARCHAR(50) NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    action_filter VARCHAR(100) NULL,
    actor_filter VARCHAR(255) NULL,
    object_type_filter VARCHAR(50) NULL,
    status VARCHAR(20) NOT NULL,
    record_count BIGINT NOT NULL,
    file_name VARCHAR(255) NULL,
    file_path VARCHAR(512) NULL,
    file_size BIGINT NULL,
    error_message TEXT NULL,
    created_at DATETIME NOT NULL,
    completed_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_activity_log_export_job_org
        FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    CONSTRAINT fk_activity_log_export_job_user
        FOREIGN KEY (requested_by) REFERENCES users(user_id),
    INDEX idx_activity_log_export_job_tenant (organization_id, id),
    INDEX idx_activity_log_export_job_status (status)
) ENGINE=InnoDB;

CREATE TABLE activity_log_export_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_id CHAR(36) NOT NULL,
    sequence_no BIGINT NOT NULL,
    occurred_at DATETIME NOT NULL,
    actor_name VARCHAR(255) NULL,
    actor_username VARCHAR(100) NULL,
    actor_role VARCHAR(50) NULL,
    action_type VARCHAR(100) NULL,
    object_type VARCHAR(50) NULL,
    object_identifier VARCHAR(255) NULL,
    before_value TEXT NULL,
    after_value TEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_activity_log_export_item_job
        FOREIGN KEY (job_id) REFERENCES activity_log_export_jobs(id) ON DELETE CASCADE,
    UNIQUE KEY uk_activity_log_export_item_sequence (job_id, sequence_no),
    INDEX idx_activity_log_export_item_job (job_id)
) ENGINE=InnoDB;
