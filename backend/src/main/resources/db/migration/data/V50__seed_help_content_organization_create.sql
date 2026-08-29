-- ============================================================
-- V37: Seed Help Content for Organization Create (NCL-01-CN-006)
--
-- Nội dung hướng dẫn tạo tổ chức mới cho screenKey 'organization-create'
-- ============================================================

INSERT INTO help_content
    (id, screen_key, role_code, title, steps, example_data, sort_order, created_at, updated_at)
VALUES
('00000000-0000-0000-0000-000000000048', 'organization-create', 'GENERAL',
 'Hướng dẫn tạo tổ chức mới',
 '["1. Mã tổ chức: Chỉ dùng A-Z, 0-9, gạch ngang và gạch dưới. Lưu ý: Mã tổ chức và tên đăng nhập không thể thay đổi sau khi tạo.", "2. Thông tin tổ chức: Nhập tên tổ chức chính xác và chọn loại hình tổ chức (Hợp tác xã, Doanh nghiệp...).", "3. Quản trị viên đầu tiên: Nhập họ tên, tên đăng nhập và email quản lý. Tài khoản quản trị này sẽ được khởi tạo đồng thời cùng tổ chức.", "4. Mật khẩu: Thiết lập mật khẩu tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt (@$!%*?&).", "5. Sau khi tạo: Bạn có thể cập nhật thêm thông tin địa chỉ, số điện thoại, email liên hệ chi tiết trong mục Hồ sơ tổ chức."]',
 NULL,
 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    steps = VALUES(steps),
    example_data = VALUES(example_data),
    updated_at = NOW();
