ALTER TABLE activity_log_export_jobs
    ADD COLUMN processing_token VARCHAR(36) NULL AFTER completed_at,
    ADD COLUMN lease_expires_at DATETIME NULL AFTER processing_token,
    ADD INDEX idx_activity_log_export_job_recovery (status, lease_expires_at, created_at);
