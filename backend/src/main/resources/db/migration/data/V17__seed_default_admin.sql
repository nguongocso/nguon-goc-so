-- ============================================================
-- V17: Seed default admin account + system organization
-- Creates exactly ONE default admin for fresh database init
-- Uses INSERT IGNORE to be safe on re-run
-- ============================================================

-- 1. Default system organization (required for admin)
INSERT IGNORE INTO organizations (organization_id, name, code, type, status, address, phone, email, created_at, updated_at)
VALUES (
    UUID(),
    'Hệ thống',
    'SYSTEM',
    'SYSTEM',
    'ACTIVE',
    'System Organization',
    NULL,
    NULL,
    NOW(),
    NOW()
);

-- 2. Default admin user
-- Username: admin
-- Password: admin123 (DEVELOPMENT ONLY — change in production!)
INSERT IGNORE INTO users (user_id, user_name, password_hash, full_name, phone, email, status, created_at, updated_at)
VALUES (
    UUID(),
    'admin',
    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
    'Quản trị viên hệ thống',
    NULL,
    NULL,
    'ACTIVE',
    NOW(),
    NOW()
);

-- 3. Link admin to system organization with ADMIN role
INSERT IGNORE INTO organization_users (id, organization_id, user_id, role_id, custom_permissions, joined_at, status)
SELECT
    UUID(),
    (SELECT organization_id FROM organizations WHERE code = 'SYSTEM' LIMIT 1),
    (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1),
    (SELECT role_id FROM roles WHERE code = 'VT-01' LIMIT 1),
    NULL,
    NOW(),
    'ACTIVE'
WHERE EXISTS (SELECT 1 FROM organizations WHERE code = 'SYSTEM')
  AND EXISTS (SELECT 1 FROM users WHERE user_name = 'admin')
  AND EXISTS (SELECT 1 FROM roles WHERE code = 'VT-01')
  AND NOT EXISTS (
    SELECT 1 FROM organization_users ou
    JOIN organizations o ON ou.organization_id = o.organization_id
    JOIN users u ON ou.user_id = u.user_id
    WHERE o.code = 'SYSTEM' AND u.user_name = 'admin'
);