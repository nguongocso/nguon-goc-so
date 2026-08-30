-- ============================================================
-- V20260830140000: Create Anomaly Thresholds Configuration Table (NCL-08-CN-014)
-- Allows VT-01 to configure global & category-specific anomaly detection thresholds
-- ============================================================

CREATE TABLE IF NOT EXISTS anomaly_thresholds (
    id CHAR(36) NOT NULL,
    product_category_id CHAR(36) NULL,
    max_scans_per_hour INT NOT NULL,
    max_scans_per_day INT NOT NULL,
    max_distance_km_per_30min DECIMAL(10,2) NOT NULL,
    min_time_between_scans_minutes INT NOT NULL,
    activation_age_days INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by CHAR(36) NOT NULL,
    updated_by CHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_threshold_category FOREIGN KEY (product_category_id) REFERENCES product_categories(id),
    CONSTRAINT fk_threshold_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_threshold_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id),
    UNIQUE KEY uk_threshold_product_category (product_category_id)
) ENGINE=InnoDB;

-- Seed initial global default threshold configuration
INSERT IGNORE INTO anomaly_thresholds (
    id, product_category_id, max_scans_per_hour, max_scans_per_day,
    max_distance_km_per_30min, min_time_between_scans_minutes, activation_age_days,
    is_active, created_by, updated_by, created_at, updated_at
)
SELECT
    UUID(), NULL, 5, 10, 50.00, 30, 365, TRUE,
    u.user_id, u.user_id, NOW(), NOW()
FROM users u
WHERE u.user_name = 'admin'
LIMIT 1;
