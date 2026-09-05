-- ============================================================
-- V53: Seed 15 partner API keys cho role VT-01 (org SYSTEM)
--
-- Bổ sung 15 khóa API key bên thứ ba thuộc tổ chức Hệ thống
-- (SYSTEM) do tài khoản 'admin' (VT-01) cấp, bên cạnh 15 khóa
-- của VT-02/DEMO_HTX đã seed ở V52. Controller
-- PartnerApiKeyController cho phép VT-01 + VT-02, org-scope theo
-- currentUser.getOrganizationId() nên 2 nhóm khóa tách riêng.
--
-- Idempotent: ID cố định (nối tiếp dải 000900000016-000900000030,
-- tránh trùng với V52) + key_hash UNIQUE + INSERT IGNORE.
-- ============================================================

INSERT IGNORE INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, total_calls, failed_calls, created_by, created_at)
SELECT
    CONCAT('00000000-0000-0000-0000-0009', LPAD(16 + t.i, 8, '0')),
    (SELECT organization_id FROM organizations WHERE code = 'SYSTEM'),
    CONCAT('Đối tác hệ thống SN-', LPAD(t.i, 2, '0')),
    CONCAT('nks_live_', SUBSTRING(t.hex, 1, 8)),
    SHA2(CONCAT('nks_live_', t.hex), 256),
    900 + t.i * 100,
    DATE_ADD(NOW(), INTERVAL 1 YEAR),
    'ACTIVE',
    0,
    0,
    (SELECT user_id FROM users WHERE user_name = 'admin'),
    NOW()
FROM (
    SELECT 1 i, 'c1d2e505c1d2e505c1d2e505c1d2e505c1d2e505c1d2e505c1d2e505c1d2e505' hex
    UNION ALL SELECT 2, 'c1d2e616c1d2e616c1d2e616c1d2e616c1d2e616c1d2e616c1d2e616c1d2e616'
    UNION ALL SELECT 3, 'c1d2e727c1d2e727c1d2e727c1d2e727c1d2e727c1d2e727c1d2e727c1d2e727'
    UNION ALL SELECT 4, 'c1d2e838c1d2e838c1d2e838c1d2e838c1d2e838c1d2e838c1d2e838c1d2e838'
    UNION ALL SELECT 5, 'c1d2e949c1d2e949c1d2e949c1d2e949c1d2e949c1d2e949c1d2e949c1d2e949'
    UNION ALL SELECT 6, 'c1d2ea5ac1d2ea5ac1d2ea5ac1d2ea5ac1d2ea5ac1d2ea5ac1d2ea5ac1d2ea5a'
    UNION ALL SELECT 7, 'c1d2eb6bc1d2eb6bc1d2eb6bc1d2eb6bc1d2eb6bc1d2eb6bc1d2eb6bc1d2eb6b'
    UNION ALL SELECT 8, 'c1d2ec7cc1d2ec7cc1d2ec7cc1d2ec7cc1d2ec7cc1d2ec7cc1d2ec7cc1d2ec7c'
    UNION ALL SELECT 9, 'c1d2ed8dc1d2ed8dc1d2ed8dc1d2ed8dc1d2ed8dc1d2ed8dc1d2ed8dc1d2ed8d'
    UNION ALL SELECT 10, 'c1d2ee9ec1d2ee9ec1d2ee9ec1d2ee9ec1d2ee9ec1d2ee9ec1d2ee9ec1d2ee9e'
    UNION ALL SELECT 11, 'c1d2efafc1d2efafc1d2efafc1d2efafc1d2efafc1d2efafc1d2efafc1d2efaf'
    UNION ALL SELECT 12, 'c1d2f0c0c1d2f0c0c1d2f0c0c1d2f0c0c1d2f0c0c1d2f0c0c1d2f0c0c1d2f0c0'
    UNION ALL SELECT 13, 'c1d2f1d1c1d2f1d1c1d2f1d1c1d2f1d1c1d2f1d1c1d2f1d1c1d2f1d1c1d2f1d1'
    UNION ALL SELECT 14, 'c1d2f2e2c1d2f2e2c1d2f2e2c1d2f2e2c1d2f2e2c1d2f2e2c1d2f2e2c1d2f2e2'
    UNION ALL SELECT 15, 'c1d2f3f3c1d2f3f3c1d2f3f3c1d2f3f3c1d2f3f3c1d2f3f3c1d2f3f3c1d2f3f3'
) t;

-- ============================================================
-- RAW API KEYS (VT-01 / SYSTEM, dùng header X-API-KEY)
--  1) nks_live_c1d2e505c1d2e505c1d2e505c1d2e505c1d2e505c1d2e505c1d2e505c1d2e505
--  2) nks_live_c1d2e616c1d2e616c1d2e616c1d2e616c1d2e616c1d2e616c1d2e616c1d2e616
--  3) nks_live_c1d2e727c1d2e727c1d2e727c1d2e727c1d2e727c1d2e727c1d2e727c1d2e727
--  4) nks_live_c1d2e838c1d2e838c1d2e838c1d2e838c1d2e838c1d2e838c1d2e838c1d2e838
--  5) nks_live_c1d2e949c1d2e949c1d2e949c1d2e949c1d2e949c1d2e949c1d2e949c1d2e949
--  6) nks_live_c1d2ea5ac1d2ea5ac1d2ea5ac1d2ea5ac1d2ea5ac1d2ea5ac1d2ea5ac1d2ea5a
--  7) nks_live_c1d2eb6bc1d2eb6bc1d2eb6bc1d2eb6bc1d2eb6bc1d2eb6bc1d2eb6bc1d2eb6b
--  8) nks_live_c1d2ec7cc1d2ec7cc1d2ec7cc1d2ec7cc1d2ec7cc1d2ec7cc1d2ec7cc1d2ec7c
--  9) nks_live_c1d2ed8dc1d2ed8dc1d2ed8dc1d2ed8dc1d2ed8dc1d2ed8dc1d2ed8dc1d2ed8d
-- 10) nks_live_c1d2ee9ec1d2ee9ec1d2ee9ec1d2ee9ec1d2ee9ec1d2ee9ec1d2ee9ec1d2ee9e
-- 11) nks_live_c1d2efafc1d2efafc1d2efafc1d2efafc1d2efafc1d2efafc1d2efafc1d2efaf
-- 12) nks_live_c1d2f0c0c1d2f0c0c1d2f0c0c1d2f0c0c1d2f0c0c1d2f0c0c1d2f0c0c1d2f0c0
-- 13) nks_live_c1d2f1d1c1d2f1d1c1d2f1d1c1d2f1d1c1d2f1d1c1d2f1d1c1d2f1d1c1d2f1d1
-- 14) nks_live_c1d2f2e2c1d2f2e2c1d2f2e2c1d2f2e2c1d2f2e2c1d2f2e2c1d2f2e2c1d2f2e2
-- 15) nks_live_c1d2f3f3c1d2f3f3c1d2f3f3c1d2f3f3c1d2f3f3c1d2f3f3c1d2f3f3c1d2f3f3
-- ============================================================