CREATE TABLE farm_log_attachments (
    id CHAR(36) NOT NULL,

    farm_log_id CHAR(36) NOT NULL,

    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,

    description TEXT,

    uploaded_by CHAR(36) NOT NULL,
    uploaded_at DATETIME,

    PRIMARY KEY (id),

    CONSTRAINT fk_farm_log_attachments_farm_log
        FOREIGN KEY (farm_log_id)
        REFERENCES farm_logs (id),

    CONSTRAINT fk_farm_log_attachments_uploaded_by
        FOREIGN KEY (uploaded_by)
        REFERENCES users (user_id)
);

CREATE INDEX idx_farm_log_attachments_farm_log_id
    ON farm_log_attachments (farm_log_id);

CREATE INDEX idx_farm_log_attachments_uploaded_by
    ON farm_log_attachments (uploaded_by);