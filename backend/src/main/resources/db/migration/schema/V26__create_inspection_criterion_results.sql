-- ============================================================
-- V24: Inspection Criterion Results
-- ============================================================

CREATE TABLE inspection_criterion_results (
    id CHAR(36) NOT NULL,
    inspection_criterion_id CHAR(36) NOT NULL,
    result_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    passed BOOLEAN NOT NULL DEFAULT TRUE,
    file_path VARCHAR(500),
    created_by CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_inspection_criterion_result
        UNIQUE (inspection_criterion_id),

    CONSTRAINT fk_inspection_criterion_result_criterion
        FOREIGN KEY (inspection_criterion_id)
        REFERENCES inspection_criteria(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_inspection_criterion_result_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(user_id)
);

-- Index cho truy vấn nhanh theo ngày hết hiệu lực
CREATE INDEX idx_inspection_criterion_result_expiry_date
    ON inspection_criterion_results(expiry_date);

-- Index cho truy vấn theo request kèm kết quả
CREATE INDEX idx_inspection_criterion_result_pass_status
    ON inspection_criterion_results(passed);
