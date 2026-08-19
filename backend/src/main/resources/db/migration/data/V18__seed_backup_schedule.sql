-- ============================================================
-- V18: Seed default backup schedule
-- Daily backup at 02:00 AM
-- ============================================================

INSERT IGNORE INTO backup_schedules (cron_expression, description, is_active, updated_by)
SELECT '0 0 2 * * ?', 'Sao lưu dữ liệu tự động hằng ngày lúc 02:00 sáng', 1, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM backup_schedules WHERE cron_expression = '0 0 2 * * ?'
);