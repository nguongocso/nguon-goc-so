-- ============================================================
-- V51: Seed 1 test account per role
--
-- VT-01 (ADMIN) đã có tài khoản 'admin' (seed ở V17, SYSTEM org)
-- nên KHÔNG tạo thêm. Tạo 1 tài khoản cho mỗi role còn lại:
--   VT-02 ORG_MANAGER   -> org COOPERATIVE (DEMO_HTX)
--   VT-03 EVENT_RECORDER -> org COOPERATIVE (DEMO_HTX)
--   VT-04 PROCUREMENT   -> org ENTERPRISE  (DEMO_NSV)
--   VT-05 REGULATOR     -> org GOVERNMENT  (DEMO_GOV)
--   VT-06 CONSUMER      -> org SYSTEM
--
-- Mật khẩu mặc định cho tất cả tài khoản: admin123 (DEVELOPMENT ONLY)
-- Hash bcrypt sao chép từ V17 (đúng hash của 'admin123').
-- Idempotent: INSERT IGNORE theo khóa UNIQUE (user_name, org code, role code).
-- ============================================================

-- 1. Demo organizations (bắt buộc để có org context cho từng role)
INSERT IGNORE INTO organizations (organization_id, name, code, type, status, address, phone, email, created_at, updated_at)
VALUES
(UUID(), 'HTX Nông Sản Demo', 'DEMO_HTX', 'COOPERATIVE', 'ACTIVE', 'Số 1 Đường Nông Sản, Hà Nội', '0912000031', 'demo-htx@nguongocso.test', NOW(), NOW()),
(UUID(), 'Công ty Nông Sản Việt Demo', 'DEMO_NSV', 'ENTERPRISE', 'ACTIVE', 'Số 2 Đường Thu Mua, TP. Hồ Chí Minh', '0912000032', 'demo-nsv@nguongocso.test', NOW(), NOW()),
(UUID(), 'Chi cục Quản lý Chất lượng Nông Sản', 'DEMO_GOV', 'GOVERNMENT', 'ACTIVE', 'Số 3 Đường Kiểm Tra, Hà Nội', '0912000033', 'demo-gov@nguongocso.test', NOW(), NOW());

-- 2. Test users (password: admin123)
INSERT IGNORE INTO users (user_id, user_name, password_hash, full_name, phone, email, status, created_at, updated_at)
VALUES
(UUID(), 'orgmanager', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Quản lý HTX Demo (VT-02)', '0912000041', 'orgmanager@demo.test', 'ACTIVE', NOW(), NOW()),
(UUID(), 'eventrecorder', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Người ghi sự kiện Demo (VT-03)', '0912000042', 'eventrecorder@demo.test', 'ACTIVE', NOW(), NOW()),
(UUID(), 'procurement', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Nhân viên thu mua Demo (VT-04)', '0912000043', 'procurement@demo.test', 'ACTIVE', NOW(), NOW()),
(UUID(), 'regulator', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Cán bộ quản lý nhà nước Demo (VT-05)', '0912000044', 'regulator@demo.test', 'ACTIVE', NOW(), NOW()),
(UUID(), 'consumer', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Người tiêu dùng Demo (VT-06)', '0912000045', 'consumer@demo.test', 'ACTIVE', NOW(), NOW());

-- 3. Link users to organizations with roles
INSERT IGNORE INTO organization_users (id, organization_id, user_id, role_id, custom_permissions, joined_at, status)
SELECT UUID(), o.organization_id, u.user_id, r.role_id, NULL, NOW(), 'ACTIVE'
FROM (
    SELECT 'orgmanager'   AS user_name, 'DEMO_HTX' AS org_code, 'VT-02' AS role_code
    UNION ALL SELECT 'eventrecorder', 'DEMO_HTX', 'VT-03'
    UNION ALL SELECT 'procurement',    'DEMO_NSV', 'VT-04'
    UNION ALL SELECT 'regulator',      'DEMO_GOV', 'VT-05'
    UNION ALL SELECT 'consumer',       'SYSTEM',   'VT-06'
) t
JOIN users u         ON u.user_name = t.user_name
JOIN organizations o ON o.code      = t.org_code
JOIN roles r         ON r.code      = t.role_code
WHERE NOT EXISTS (
    SELECT 1
    FROM organization_users ou
    JOIN users         u2 ON ou.user_id = u2.user_id
    JOIN organizations o2 ON ou.organization_id = o2.organization_id
    WHERE u2.user_name = t.user_name
      AND o2.code      = t.org_code
);