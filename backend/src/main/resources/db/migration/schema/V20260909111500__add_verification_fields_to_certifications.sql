-- ============================================================
-- V20260909111500: Bổ sung các trường xác thực chứng nhận cho VT-01
-- Jira Story: NCL-696, Backlog: NCL-09-CN-012, Quy tắc: QTN-34
-- ============================================================

ALTER TABLE certifications
    ADD COLUMN verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN reviewed_by CHAR(36) NULL,
    ADD COLUMN reviewed_at DATETIME NULL,
    ADD COLUMN review_note VARCHAR(1000) NULL,
    ADD COLUMN rejection_reason VARCHAR(1000) NULL,
    ADD COLUMN document_file_name VARCHAR(255) NULL,
    ADD COLUMN document_content_type VARCHAR(100) NULL,
    ADD COLUMN document_file_size BIGINT NULL,
    ADD COLUMN document_storage_path VARCHAR(500) NULL,
    ADD CONSTRAINT fk_cert_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(user_id);
