-- Seed permissions and role_permissions for Input Material management (US NCL-09-CN-010)

INSERT IGNORE INTO permissions (resource, action, description) VALUES
('input_material', 'CREATE', 'Tạo vật tư đầu vào'),
('input_material', 'READ', 'Xem danh mục vật tư đầu vào'),
('input_material', 'UPDATE', 'Cập nhật vật tư đầu vào'),
('input_material', 'DELETE', 'Xóa vật tư đầu vào');

-- Grant all input_material permissions to ADMIN (VT-01, role_id = 1)
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), 1, p.permission_id, TRUE, NOW()
FROM permissions p
WHERE p.resource = 'input_material'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 1 AND rp.permission_id = p.permission_id
);

-- Grant READ input_material permission to ORG_MANAGER (VT-02), EVENT_RECORDER (VT-03), PROCUREMENT (VT-04), REGULATOR (VT-05)
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), r.role_id, p.permission_id, TRUE, NOW()
FROM permissions p
CROSS JOIN roles r
WHERE p.resource = 'input_material' AND p.action = 'READ'
AND r.role_id IN (2, 3, 4, 5)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.role_id AND rp.permission_id = p.permission_id
);
