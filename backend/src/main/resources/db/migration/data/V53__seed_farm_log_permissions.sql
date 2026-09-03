-- Seed / Assign farm_log & FARM_LOG permissions for VT-02 (Cooperative Manager) and VT-03 (Recorder)
-- Ensures permissions exist in permissions table
INSERT IGNORE INTO permissions (resource, action, description) VALUES
('FARM_LOG', 'CREATE', 'Tạo nhật ký canh tác'),
('FARM_LOG', 'READ', 'Xem nhật ký canh tác'),
('FARM_LOG', 'UPDATE', 'Đính chính nhật ký canh tác'),
('FARM_LOG', 'VERIFY', 'Xác minh nhật ký canh tác'),
('farm_log', 'CREATE', 'Tạo nhật ký canh tác'),
('farm_log', 'READ', 'Xem nhật ký canh tác'),
('farm_log', 'UPDATE', 'Đính chính nhật ký canh tác'),
('farm_log', 'VERIFY', 'Xác minh nhật ký canh tác');

-- Grant permissions to VT-02 (ORG_MANAGER, role_id = 2)
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), 2, p.permission_id, TRUE, NOW()
FROM permissions p
WHERE p.resource IN ('FARM_LOG', 'farm_log') AND p.action IN ('CREATE', 'READ', 'UPDATE', 'VERIFY')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 2 AND rp.permission_id = p.permission_id
);

-- Grant permissions to VT-03 (EVENT_RECORDER, role_id = 3)
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), 3, p.permission_id, TRUE, NOW()
FROM permissions p
WHERE p.resource IN ('FARM_LOG', 'farm_log') AND p.action IN ('CREATE', 'READ', 'UPDATE')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 3 AND rp.permission_id = p.permission_id
);

