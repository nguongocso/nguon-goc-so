-- ============================================================
-- V20260911100000: Create System Configurations Table
-- Cho phép cấu hình các tham số hệ thống động (NCL-11-CN-004)
-- ============================================================

CREATE TABLE IF NOT EXISTS system_configurations (
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    updated_by CHAR(36) NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (config_key),
    CONSTRAINT fk_system_configs_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Seed cấu hình ngưỡng cảnh báo hết hiệu lực kiểm nghiệm mặc định 15 ngày
INSERT IGNORE INTO system_configurations (config_key, config_value, description)
VALUES ('INSPECTION_EXPIRY_WARNING_THRESHOLD_DAYS', '15', 'Ngưỡng số ngày cảnh báo kết quả kiểm nghiệm sắp hết hiệu lực (mặc định 15 ngày)');
