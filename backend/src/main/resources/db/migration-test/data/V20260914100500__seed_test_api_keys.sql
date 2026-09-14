-- ============================================================
-- V20260914100500: Seed khóa thử nghiệm mẫu (NCL-12-CN-004)
-- Phục vụ kiểm thử TC-01, TC-02, TC-03 trong môi trường test
-- ============================================================

-- 1. Khóa thử nghiệm đang hoạt động (TC-01, TC-02)
-- RAW KEY: nks_test_e8a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1
INSERT IGNORE INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000099',
    (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX'),
    'Đối tác Thử nghiệm Sandbox (Đang hoạt động)',
    'nks_test_e8a1b2c3',
    SHA2('nks_test_e8a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1', 256),
    60,
    DATE_ADD(NOW(), INTERVAL 30 DAY),
    'ACTIVE',
    TRUE,
    0,
    0,
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE code = 'DEMO_HTX');

-- 2. Khóa thử nghiệm đã hết hạn (TC-03)
-- RAW KEY: nks_test_expired1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1
INSERT IGNORE INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000098',
    (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX'),
    'Đối tác Thử nghiệm (Đã hết hạn)',
    'nks_test_expired1',
    SHA2('nks_test_expired1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1', 256),
    60,
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    'ACTIVE',
    TRUE,
    0,
    0,
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE code = 'DEMO_HTX');
