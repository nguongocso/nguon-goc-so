INSERT IGNORE INTO permissions(resource, action, description)
VALUES ('shipment', 'SPLIT', 'Tách lô hàng cho nhiều đối tác');

INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), r.role_id, p.permission_id, TRUE, NOW()
FROM roles r
JOIN permissions p ON p.resource = 'shipment' AND p.action = 'SPLIT'
WHERE r.code = 'VT-02';
