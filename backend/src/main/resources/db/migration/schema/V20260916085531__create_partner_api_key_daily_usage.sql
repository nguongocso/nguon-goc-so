-- ============================================================
-- V20260916085531: Bảng đếm lượt gọi theo ngày của khóa truy cập đối tác
-- (NCL-12-CN-005 - Cảnh báo khóa truy cập sắp hết hạn và sắp chạm hạn mức)
--
-- Phụ thuộc: partner_api_keys
--
-- Bảng là nguồn sự thật cho ngưỡng cảnh báo hạn mức: một dòng cho mỗi
-- khóa x ngày. Nhờ lưu ở DB, cảnh báo không mất khi khởi động lại backend
-- và không gửi trùng khi chạy nhiều instance.
--   call_count       : số lượt gọi đã xác thực thành công trong ngày
--   warning_sent_at  : mốc gửi cảnh báo (NULL = chưa gửi) - cờ claim chống trùng
-- ============================================================

CREATE TABLE partner_api_key_daily_usage (
    id CHAR(36) NOT NULL,
    api_key_id CHAR(36) NOT NULL,
    usage_date DATE NOT NULL,
    call_count INT NOT NULL DEFAULT 0,
    warning_sent_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_partner_api_key_daily_usage PRIMARY KEY (id),
    CONSTRAINT uq_partner_api_key_daily_usage UNIQUE (api_key_id, usage_date),
    CONSTRAINT fk_partner_api_key_daily_usage_key
        FOREIGN KEY (api_key_id) REFERENCES partner_api_keys (id) ON DELETE CASCADE,
    INDEX idx_partner_api_key_daily_usage_date (usage_date, warning_sent_at)
) ENGINE=InnoDB;