-- ============================================================
-- V41: NCL-03-CN-006 - Đính chính nhật ký canh tác
-- Thêm các cột liên kết bản đính chính tới bản gốc trên farm_logs:
--   original_farm_log_id : FK trỏ về farm_logs.id (bản gốc)
--   is_correction        : đánh dấu đây là bản ghi đính chính
--   correction_reason    : lý do đính chính (bắt buộc với bản đính chính)
--   corrected_by         : người thực hiện đính chính (FK users)
--   is_corrected         : đánh dấu bản ghi đã bị thay thế hiệu lực
-- ============================================================

ALTER TABLE farm_logs
    ADD COLUMN original_farm_log_id CHAR(36) NULL,
    ADD COLUMN is_correction BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN correction_reason TEXT NULL,
    ADD COLUMN corrected_by CHAR(36) NULL,
    ADD COLUMN is_corrected BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE farm_logs
    ADD CONSTRAINT fk_farm_logs_original_farm_log
    FOREIGN KEY (original_farm_log_id) REFERENCES farm_logs(id);

ALTER TABLE farm_logs
    ADD CONSTRAINT fk_farm_logs_corrected_by
    FOREIGN KEY (corrected_by) REFERENCES users(user_id);

CREATE INDEX idx_farm_logs_original_farm_log ON farm_logs(original_farm_log_id);
CREATE INDEX idx_farm_logs_corrected_by ON farm_logs(corrected_by);
