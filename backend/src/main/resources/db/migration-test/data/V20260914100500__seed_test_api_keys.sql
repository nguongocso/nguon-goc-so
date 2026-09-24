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
    'EXPIRED',
    TRUE,
    0,
    0,
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE code = 'DEMO_HTX');

-- 3. Khóa truy cập chính thức (Live Key) đã hết thời gian hiệu lực
-- RAW KEY: nks_live_expired1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1
INSERT IGNORE INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000097',
    (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX'),
    'Đối tác Doanh Nghiệp (Khóa Live đã hết hạn)',
    'nks_live_expired1',
    SHA2('nks_live_expired1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1', 256),
    500,
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    'EXPIRED',
    FALSE,
    0,
    0,
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE code = 'DEMO_HTX');

-- 4. Khóa thử nghiệm đã bị thu hồi
-- RAW KEY: nks_test_revoked1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1
INSERT IGNORE INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000096',
    (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX'),
    'Đối tác Thử nghiệm (Đã bị thu hồi)',
    'nks_test_revoked1',
    SHA2('nks_test_revoked1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1', 256),
    30,
    DATE_ADD(NOW(), INTERVAL 15 DAY),
    'REVOKED',
    TRUE,
    0,
    0,
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE code = 'DEMO_HTX');

-- 5. Khóa thử nghiệm giới hạn 1 lượt/giờ (Test 429 Rate Limit)
-- RAW KEY: nks_test_ratelimitd4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0
INSERT IGNORE INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000095',
    (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX'),
    'Đối tác Thử nghiệm (Hạn mức 1 lượt/giờ)',
    'nks_test_ratelimi',
    SHA2('nks_test_ratelimitd4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0', 256),
    1,
    DATE_ADD(NOW(), INTERVAL 15 DAY),
    'ACTIVE',
    TRUE,
    0,
    0,
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE code = 'DEMO_HTX');

-- 6. Khóa chính thức đang hoạt động
-- RAW KEY: nks_live_active1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1
INSERT IGNORE INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000094',
    (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX'),
    'Doanh Nghiệp Thu Mua Nông Sản Sạch (Khóa Live đang hoạt động)',
    'nks_live_active1',
    SHA2('nks_live_active1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1', 256),
    1000,
    DATE_ADD(NOW(), INTERVAL 365 DAY),
    'ACTIVE',
    FALSE,
    0,
    0,
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE code = 'DEMO_HTX');
