-- ============================================================
-- V20260914110200: Tạo bảng nhật ký xuất hồ sơ (export_logs)
-- User Story: NCL-07-CN-007
-- ============================================================

CREATE TABLE IF NOT EXISTS export_logs (
    id CHAR(36) NOT NULL,
    shipment_id CHAR(36) NOT NULL,
    template_id CHAR(36) NULL,
    exported_by CHAR(36) NOT NULL,
    exported_at DATETIME NOT NULL,
    CONSTRAINT pk_export_logs PRIMARY KEY (id),
    CONSTRAINT fk_export_log_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id),
    CONSTRAINT fk_export_log_template FOREIGN KEY (template_id) REFERENCES profile_templates (id) ON DELETE SET NULL,
    CONSTRAINT fk_export_log_user FOREIGN KEY (exported_by) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_export_logs_shipment ON export_logs (shipment_id);
CREATE INDEX idx_export_logs_template ON export_logs (template_id);
