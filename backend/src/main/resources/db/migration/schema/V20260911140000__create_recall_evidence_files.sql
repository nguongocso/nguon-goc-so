-- Migration: Tạo bảng lưu trữ tệp biên bản thu hồi (hỗ trợ file PDF, DOCX) - NCL-08-CN-012
CREATE TABLE IF NOT EXISTS recall_evidence_files (
    id CHAR(36) NOT NULL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    uploaded_by CHAR(36) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_recall_evidence_user FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
);
