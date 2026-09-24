-- ============================================================
-- V20260923160000: Seed dữ liệu khóa API thử nghiệm và chính thức (NCL-12-CN-004)
-- Phục vụ kiểm thử các kịch bản mã lỗi HTTP: 400, 401, 403, 404, 429, 500
-- ============================================================

-- 1. Khóa thử nghiệm đang hoạt động (ACTIVE Sandbox Key - Dùng test 200 OK & 403 khi gọi ngoài Sandbox)
-- RAW KEY: nks_test_e8a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1
-- PREFIX: nks_test_e8a1b2c3
INSERT INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000099',
    COALESCE(
        (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX' LIMIT 1),
        (SELECT organization_id FROM organizations WHERE type = 'COOPERATIVE' LIMIT 1),
        (SELECT organization_id FROM organizations WHERE code = 'SYSTEM' LIMIT 1),
        (SELECT organization_id FROM organizations ORDER BY created_at ASC LIMIT 1)
    ),
    'Đối tác Thử nghiệm Sandbox (Đang hoạt động)',
    'nks_test_e8a1b2c3',
    SHA2('nks_test_e8a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1', 256),
    50,
    DATE_ADD(NOW(), INTERVAL 15 DAY),
    'ACTIVE',
    TRUE,
    0,
    0,
    COALESCE(
        (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
        (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1),
        (SELECT user_id FROM users ORDER BY created_at ASC LIMIT 1)
    ),
    NOW()
ON DUPLICATE KEY UPDATE
    partner_name = VALUES(partner_name),
    status = VALUES(status),
    expires_at = VALUES(expires_at),
    rate_limit_per_hour = VALUES(rate_limit_per_hour),
    is_test = VALUES(is_test);

-- 2. Khóa thử nghiệm đã hết hạn (EXPIRED Test Key - Dùng test 401: "Khóa thử nghiệm đã hết hạn")
-- RAW KEY: nks_test_expired1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1
-- PREFIX: nks_test_expired1
INSERT INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000098',
    COALESCE(
        (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX' LIMIT 1),
        (SELECT organization_id FROM organizations WHERE type = 'COOPERATIVE' LIMIT 1),
        (SELECT organization_id FROM organizations WHERE code = 'SYSTEM' LIMIT 1),
        (SELECT organization_id FROM organizations ORDER BY created_at ASC LIMIT 1)
    ),
    'Đối tác Thử nghiệm (Khóa thử nghiệm đã hết hạn - Test 401)',
    'nks_test_expired1',
    SHA2('nks_test_expired1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1', 256),
    30,
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    'EXPIRED',
    TRUE,
    0,
    0,
    COALESCE(
        (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
        (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1),
        (SELECT user_id FROM users ORDER BY created_at ASC LIMIT 1)
    ),
    NOW()
ON DUPLICATE KEY UPDATE
    partner_name = VALUES(partner_name),
    status = VALUES(status),
    expires_at = VALUES(expires_at),
    rate_limit_per_hour = VALUES(rate_limit_per_hour),
    is_test = VALUES(is_test);

-- 3. Khóa truy cập chính thức (Live Key) đã hết thời gian hiệu lực (Dùng test 401: "Khóa truy cập đã hết thời gian hiệu lực")
-- RAW KEY: nks_live_expired1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1
-- PREFIX: nks_live_expired1
INSERT INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000097',
    COALESCE(
        (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX' LIMIT 1),
        (SELECT organization_id FROM organizations WHERE type = 'COOPERATIVE' LIMIT 1),
        (SELECT organization_id FROM organizations WHERE code = 'SYSTEM' LIMIT 1),
        (SELECT organization_id FROM organizations ORDER BY created_at ASC LIMIT 1)
    ),
    'Đối tác Doanh Nghiệp (Khóa Live đã hết hạn - Test 401)',
    'nks_live_expired1',
    SHA2('nks_live_expired1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1', 256),
    500,
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    'EXPIRED',
    FALSE,
    0,
    0,
    COALESCE(
        (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
        (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1),
        (SELECT user_id FROM users ORDER BY created_at ASC LIMIT 1)
    ),
    NOW()
ON DUPLICATE KEY UPDATE
    partner_name = VALUES(partner_name),
    status = VALUES(status),
    expires_at = VALUES(expires_at),
    rate_limit_per_hour = VALUES(rate_limit_per_hour),
    is_test = VALUES(is_test);

-- 4. Khóa API đã bị Quản lý Hợp tác xã thu hồi (Dùng test 401: "Khóa truy cập đã bị thu hồi và không còn hiệu lực")
-- RAW KEY: nks_test_revoked1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1
-- PREFIX: nks_test_revoked1
INSERT INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000096',
    COALESCE(
        (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX' LIMIT 1),
        (SELECT organization_id FROM organizations WHERE type = 'COOPERATIVE' LIMIT 1),
        (SELECT organization_id FROM organizations WHERE code = 'SYSTEM' LIMIT 1),
        (SELECT organization_id FROM organizations ORDER BY created_at ASC LIMIT 1)
    ),
    'Đối tác Thử nghiệm (Khóa thử nghiệm đã bị thu hồi - Test 401)',
    'nks_test_revoked1',
    SHA2('nks_test_revoked1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1', 256),
    30,
    DATE_ADD(NOW(), INTERVAL 15 DAY),
    'REVOKED',
    TRUE,
    0,
    0,
    COALESCE(
        (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
        (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1),
        (SELECT user_id FROM users ORDER BY created_at ASC LIMIT 1)
    ),
    NOW()
ON DUPLICATE KEY UPDATE
    partner_name = VALUES(partner_name),
    status = VALUES(status),
    expires_at = VALUES(expires_at),
    rate_limit_per_hour = VALUES(rate_limit_per_hour),
    is_test = VALUES(is_test);

-- 5. Khóa thử nghiệm bị giới hạn hạn mức thấp 1 lượt/giờ (Dùng test 429: "Khóa truy cập đã vượt quá hạn mức 1 lượt gọi/giờ")
-- RAW KEY: nks_test_ratelimitd4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0
-- PREFIX: nks_test_ratelimi
INSERT INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000095',
    COALESCE(
        (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX' LIMIT 1),
        (SELECT organization_id FROM organizations WHERE type = 'COOPERATIVE' LIMIT 1),
        (SELECT organization_id FROM organizations WHERE code = 'SYSTEM' LIMIT 1),
        (SELECT organization_id FROM organizations ORDER BY created_at ASC LIMIT 1)
    ),
    'Đối tác Thử nghiệm (Hạn mức thấp 1 lượt/giờ - Test 429)',
    'nks_test_ratelimi',
    SHA2('nks_test_ratelimitd4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0', 256),
    1,
    DATE_ADD(NOW(), INTERVAL 15 DAY),
    'ACTIVE',
    TRUE,
    0,
    0,
    COALESCE(
        (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
        (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1),
        (SELECT user_id FROM users ORDER BY created_at ASC LIMIT 1)
    ),
    NOW()
ON DUPLICATE KEY UPDATE
    partner_name = VALUES(partner_name),
    status = VALUES(status),
    expires_at = VALUES(expires_at),
    rate_limit_per_hour = VALUES(rate_limit_per_hour),
    is_test = VALUES(is_test);

-- 6. Khóa truy cập chính thức (Live Key) đang hoạt động (Dùng test 200 OK lô thật, 404 khi lô không tồn tại, 400 khi sai định dạng)
-- RAW KEY: nks_live_active1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1
-- PREFIX: nks_live_active1
INSERT INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000094',
    COALESCE(
        (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX' LIMIT 1),
        (SELECT organization_id FROM organizations WHERE type = 'COOPERATIVE' LIMIT 1),
        (SELECT organization_id FROM organizations WHERE code = 'SYSTEM' LIMIT 1),
        (SELECT organization_id FROM organizations ORDER BY created_at ASC LIMIT 1)
    ),
    'Doanh Nghiệp Thu Mua Nông Sản Sạch (Khóa Live đang hoạt động - Test 200/404/400)',
    'nks_live_active1',
    SHA2('nks_live_active1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1', 256),
    1000,
    DATE_ADD(NOW(), INTERVAL 365 DAY),
    'ACTIVE',
    FALSE,
    0,
    0,
    COALESCE(
        (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
        (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1),
        (SELECT user_id FROM users ORDER BY created_at ASC LIMIT 1)
    ),
    NOW()
ON DUPLICATE KEY UPDATE
    partner_name = VALUES(partner_name),
    status = VALUES(status),
    expires_at = VALUES(expires_at),
    rate_limit_per_hour = VALUES(rate_limit_per_hour),
    is_test = VALUES(is_test);

-- 7. Khóa truy cập chính thức (Live Key) đã bị thu hồi (Dùng test 401: "Khóa truy cập đã bị thu hồi và không còn hiệu lực")
-- RAW KEY: nks_live_revoked1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1
-- PREFIX: nks_live_revoked1
INSERT INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000093',
    COALESCE(
        (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX' LIMIT 1),
        (SELECT organization_id FROM organizations WHERE type = 'COOPERATIVE' LIMIT 1),
        (SELECT organization_id FROM organizations WHERE code = 'SYSTEM' LIMIT 1),
        (SELECT organization_id FROM organizations ORDER BY created_at ASC LIMIT 1)
    ),
    'Đối tác Doanh Nghiệp (Khóa Live đã bị thu hồi - Test 401)',
    'nks_live_revoked1',
    SHA2('nks_live_revoked1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1', 256),
    500,
    DATE_ADD(NOW(), INTERVAL 30 DAY),
    'REVOKED',
    FALSE,
    0,
    0,
    COALESCE(
        (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
        (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1),
        (SELECT user_id FROM users ORDER BY created_at ASC LIMIT 1)
    ),
    NOW()
ON DUPLICATE KEY UPDATE
    partner_name = VALUES(partner_name),
    status = VALUES(status),
    expires_at = VALUES(expires_at),
    rate_limit_per_hour = VALUES(rate_limit_per_hour),
    is_test = VALUES(is_test);
