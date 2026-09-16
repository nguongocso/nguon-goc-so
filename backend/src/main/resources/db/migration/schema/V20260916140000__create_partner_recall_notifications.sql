-- ============================================================
-- V20260916140000: Tạo bảng theo dõi truy xuất lô đối tác và lịch sử thông báo thu hồi webhook
-- User Story: NCL-12-CN-006 - Thông báo tự động tới bên thứ ba khi lô bị thu hồi
-- ============================================================

-- Bảng 1: Nhật ký truy xuất dữ liệu lô của đối tác (TC-03)
CREATE TABLE IF NOT EXISTS partner_lot_access_logs (
    id CHAR(36) NOT NULL,
    partner_api_key_id CHAR(36) NOT NULL,
    shipment_id CHAR(36) NULL,
    production_lot_id CHAR(36) NULL,
    accessed_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_plal_partner_key FOREIGN KEY (partner_api_key_id) REFERENCES partner_api_keys(id) ON DELETE CASCADE,
    CONSTRAINT fk_plal_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE,
    CONSTRAINT fk_plal_lot FOREIGN KEY (production_lot_id) REFERENCES production_lot(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_plal_shipment_time ON partner_lot_access_logs (shipment_id, accessed_at);
CREATE INDEX idx_plal_lot_time ON partner_lot_access_logs (production_lot_id, accessed_at);
CREATE INDEX idx_plal_key_time ON partner_lot_access_logs (partner_api_key_id, accessed_at);

-- Bảng 2: Lịch sử gửi thông báo thu hồi và lịch sử thử lại giãn dần (TC-01, TC-02, TC-04)
CREATE TABLE IF NOT EXISTS partner_webhook_notifications (
    id CHAR(36) NOT NULL,
    partner_api_key_id CHAR(36) NOT NULL,
    shipment_id CHAR(36) NOT NULL,
    lot_code VARCHAR(100) NOT NULL,
    new_status VARCHAR(30) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    public_reason TEXT NOT NULL,
    payload JSON NOT NULL,
    delivery_status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    next_retry_at DATETIME NULL,
    last_http_status INT NULL,
    last_error_message TEXT NULL,
    attempts_log JSON NULL,
    created_at DATETIME NOT NULL,
    completed_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pwn_partner_key FOREIGN KEY (partner_api_key_id) REFERENCES partner_api_keys(id) ON DELETE CASCADE,
    CONSTRAINT fk_pwn_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_pwn_retry ON partner_webhook_notifications (delivery_status, next_retry_at);
CREATE INDEX idx_pwn_key_created ON partner_webhook_notifications (partner_api_key_id, created_at);
