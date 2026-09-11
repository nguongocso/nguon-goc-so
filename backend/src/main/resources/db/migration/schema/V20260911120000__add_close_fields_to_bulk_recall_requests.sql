-- ============================================================
-- V20260911120000: Bổ sung các trường kết thúc vụ việc cho bulk_recall_requests (NCL-08-CN-012)
-- Lưu trữ biện pháp khắc phục phòng ngừa và thông tin kết thúc vụ việc trực tiếp trên yêu cầu thu hồi
-- ============================================================

ALTER TABLE bulk_recall_requests
    ADD COLUMN remediation_measures TEXT NULL COMMENT 'Biện pháp khắc phục phòng ngừa chung (QTN-27)',
    ADD COLUMN evidence_file_ids TEXT NULL COMMENT 'Danh sách UUID tệp biên bản/bằng chứng',
    ADD COLUMN closed_at DATETIME NULL COMMENT 'Thời điểm kết thúc vụ việc',
    ADD COLUMN closed_by CHAR(36) NULL COMMENT 'Người thực hiện kết thúc vụ việc';

ALTER TABLE bulk_recall_requests
    ADD CONSTRAINT fk_bulk_recall_closed_by
        FOREIGN KEY (closed_by) REFERENCES users(user_id);
