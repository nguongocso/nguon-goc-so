-- ============================================================
-- V20260916140500: Bổ sung các trường webhook cho partner_api_keys và cấu hình thời gian thông báo thu hồi
-- User Story: NCL-12-CN-006
-- ============================================================

ALTER TABLE partner_api_keys
    ADD COLUMN webhook_url VARCHAR(500) NULL AFTER last_call_ip,
    ADD COLUMN webhook_secret VARCHAR(64) NULL AFTER webhook_url,
    ADD COLUMN is_webhook_active BOOLEAN NOT NULL DEFAULT TRUE AFTER webhook_secret;

INSERT IGNORE INTO system_configurations (config_key, config_value, description)
VALUES ('PARTNER_RECALL_NOTIFICATION_WINDOW_DAYS', '30', 'Khoảng thời gian (ngày) để lọc đối tác đã từng lấy dữ liệu của lô bị thu hồi');
