-- ============================================================
-- V20260909090000: Seed permission "recall:UPDATE" (NCL-08-CN-011)
--
-- Sửa lỗi thực tế khi phê duyệt/từ chối yêu cầu thu hồi theo phạm vi
-- ảnh hưởng: UI hiển thị "Permission không tồn tại.".
--
-- Nguyên nhân: BulkRecallRequestController gọi
-- permissionChecker.check("recall", "UPDATE") cho approve/reject,
-- nhưng permission "recall:UPDATE" chưa được seed (V15 chỉ có
-- "recall:CREATE" và "recall:READ") nên PermissionCheckerImpl
-- không tìm thấy permission trong bảng permissions và ném
-- BusinessException("Permission không tồn tại.") trước khi mọi
-- business rule (QTN-22, cùng tổ chức, PENDING) được kiểm tra.
--
-- Quyền "recall:UPDATE" đại diện cho hành động xử lý yêu cầu thu hồi
-- (phê duyệt / từ chối). Cấp cho:
-- - VT-02 (Quản lý HTX): chủ thể phê duyệt/từ chối theo nghiệp vụ
--   NCL-08-CN-011. Business rule vẫn enforce ở service layer:
--   người duyệt khác người tạo (QTN-22) và cùng tổ chức với lô.
-- - VT-01 (Admin): toàn quyền hệ thống theo convention V16/V39.
--
-- Idempotent: INSERT IGNORE + NOT EXISTS, không tạo duplicate.
-- ============================================================

INSERT IGNORE INTO permissions (resource, action, description)
VALUES ('recall', 'UPDATE', 'Phê duyệt / từ chối yêu cầu thu hồi');

-- ADMIN (VT-01): toàn quyền hệ thống
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), r.role_id, p.permission_id, TRUE, NOW()
FROM roles r
JOIN permissions p
  ON p.resource = 'recall' AND p.action = 'UPDATE'
WHERE r.code = 'VT-01'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.role_id
        AND rp.permission_id = p.permission_id
  );

-- ORG_MANAGER (VT-02): phê duyệt / từ chối yêu cầu thu hồi
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), r.role_id, p.permission_id, TRUE, NOW()
FROM roles r
JOIN permissions p
  ON p.resource = 'recall' AND p.action = 'UPDATE'
WHERE r.code = 'VT-02'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.role_id
        AND rp.permission_id = p.permission_id
  );
