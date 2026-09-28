-- ============================================================
-- V20260914101000: Seed khóa API thử nghiệm mẫu cho Cổng dữ liệu (NCL-12-CN-004)
-- Dùng để bên thứ ba kiểm thử ngay với lệnh curl trong tài liệu hướng dẫn
-- ============================================================

-- 1. Khóa thử nghiệm mẫu mặc định trong tài liệu cổng dữ liệu
-- RAW KEY: nks_test_sample_key_1234567890
-- PREFIX: nks_test_samp
INSERT INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000001',
    COALESCE(
        (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX' LIMIT 1),
        (SELECT organization_id FROM organizations ORDER BY created_at ASC LIMIT 1)
    ),
    'Đối tác Thử nghiệm Mẫu (Sandbox Documentation Key)',
    'nks_test_samp',
    SHA2('nks_test_sample_key_1234567890', 256),
    100,
    DATE_ADD(NOW(), INTERVAL 365 DAY),
    'ACTIVE',
    TRUE,
    0,
    0,
    COALESCE(
        (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
        (SELECT user_id FROM users ORDER BY created_at ASC LIMIT 1)
    ),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM partner_api_keys WHERE id = '00000000-0000-0000-0000-000900000001'
);

-- 2. Khóa thử nghiệm phụ phục vụ kiểm thử tích hợp (TC-01, TC-02)
-- RAW KEY: nks_test_e8a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1
-- PREFIX: nks_test_e8a1b2c3
INSERT INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000099',
    COALESCE(
        (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX' LIMIT 1),
        (SELECT organization_id FROM organizations ORDER BY created_at ASC LIMIT 1)
    ),
    'Đối tác Thử nghiệm Sandbox (Đang hoạt động)',
    'nks_test_e8a1b2c3',
    SHA2('nks_test_e8a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1', 256),
    100,
    DATE_ADD(NOW(), INTERVAL 30 DAY),
    'ACTIVE',
    TRUE,
    0,
    0,
    COALESCE(
        (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
        (SELECT user_id FROM users ORDER BY created_at ASC LIMIT 1)
    ),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM partner_api_keys WHERE id = '00000000-0000-0000-0000-000900000099'
);
