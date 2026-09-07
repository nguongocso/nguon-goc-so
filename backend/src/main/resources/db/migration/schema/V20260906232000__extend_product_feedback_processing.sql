-- NCL-08-CN-009: nền tảng dữ liệu cho luồng xử lý phản ánh người tiêu dùng.
ALTER TABLE product_feedbacks
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    ADD COLUMN severity VARCHAR(32) NOT NULL DEFAULT 'INFORMATION',
    ADD COLUMN trace_code_id CHAR(36) NULL,
    ADD COLUMN assigned_to CHAR(36) NULL,
    ADD COLUMN assigned_at DATETIME NULL,
    ADD COLUMN processing_content TEXT NULL,
    ADD COLUMN public_response TEXT NULL,
    ADD COLUMN close_reason TEXT NULL,
    ADD COLUMN closed_by CHAR(36) NULL,
    ADD COLUMN closed_at DATETIME NULL,
    ADD COLUMN updated_at DATETIME NULL;

UPDATE product_feedbacks
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE product_feedbacks
    MODIFY COLUMN updated_at DATETIME NOT NULL,
    ADD CONSTRAINT fk_product_feedback_assigned_to
        FOREIGN KEY (assigned_to) REFERENCES users(user_id),
    ADD CONSTRAINT fk_product_feedback_closed_by
        FOREIGN KEY (closed_by) REFERENCES users(user_id),
    ADD CONSTRAINT fk_product_feedback_trace_code
        FOREIGN KEY (trace_code_id) REFERENCES trace_codes(id);

ALTER TABLE recall_requests
    ADD COLUMN source_feedback_id CHAR(36) NULL,
    ADD CONSTRAINT fk_recall_request_source_feedback
        FOREIGN KEY (source_feedback_id) REFERENCES product_feedbacks(id);

CREATE INDEX idx_product_feedback_status_created
    ON product_feedbacks(status, created_at);

CREATE INDEX idx_product_feedback_severity_created
    ON product_feedbacks(severity, created_at);

CREATE INDEX idx_product_feedback_assigned_status
    ON product_feedbacks(assigned_to, status);

CREATE INDEX idx_product_feedback_trace_code
    ON product_feedbacks(trace_code_id);

CREATE INDEX idx_recall_request_feedback_status
    ON recall_requests(source_feedback_id, status);
