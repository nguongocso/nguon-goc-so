-- Seed / Assign farm_log permissions for VT-02 (Cooperative Manager) and VT-03 (Recorder)
-- Ensures farm_log permissions exist in permissions table
INSERT IGNORE INTO permissions (resource, action, description) VALUES
('farm_log', 'CREATE', 'Tạo nhật ký'),
('farm_log', 'READ', 'Xem nhật ký'),
('farm_log', 'UPDATE', 'Cập nhật nhật ký'),
('farm_log', 'VERIFY', 'Xác minh nhật ký');

-- Grant farm_log permissions to VT-02 (ORG_MANAGER, role_id = 2)
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), 2, p.permission_id, TRUE, NOW()
FROM permissions p
WHERE p.resource = 'farm_log' AND p.action IN ('CREATE', 'READ', 'UPDATE', 'VERIFY')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 2 AND rp.permission_id = p.permission_id
);

-- Grant farm_log permissions to VT-03 (EVENT_RECORDER, role_id = 3)
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), 3, p.permission_id, TRUE, NOW()
FROM permissions p
WHERE p.resource = 'farm_log' AND p.action IN ('CREATE', 'READ', 'UPDATE')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 3 AND rp.permission_id = p.permission_id
);
