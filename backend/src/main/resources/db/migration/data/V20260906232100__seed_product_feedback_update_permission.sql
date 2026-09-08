-- NCL-08-CN-009: chỉ quản lý tổ chức (VT-02) được xử lý phản ánh.
INSERT IGNORE INTO permissions(resource, action, description)
VALUES ('product_feedback', 'UPDATE', 'Xử lý phản ánh sản phẩm');

INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), r.role_id, p.permission_id, TRUE, NOW()
FROM roles r
JOIN permissions p
  ON p.resource = 'product_feedback' AND p.action = 'UPDATE'
WHERE r.code = 'VT-02'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.role_id
        AND rp.permission_id = p.permission_id
  );
