-- ============================================================
-- V67: Seed Help Content - Quản lý mốc canh tác (NCL-675)
--
-- Bổ sung nội dung hướng dẫn cho 3 màn hình của chức năng Mốc
-- canh tác (danh sách, tạo mới, cập nhật) với chung screenKey
-- 'admin-cultivation-milestones':
--   - /admin/cultivation-milestones         (CultivationMilestoneManagementPage)
--   - /admin/cultivation-milestones/create  (CreateCultivationMilestonePage)
--   - /admin/cultivation-milestones/:id/edit(EditCultivationMilestonePage)
-- Trước đó chưa có dữ liệu nên drawer hướng dẫn hiển thị
-- "Chưa có hướng dẫn cho màn hình này".
-- ============================================================

INSERT INTO help_content
    (id, screen_key, role_code, title, steps, example_data, sort_order, created_at, updated_at)
VALUES
('00000000-0000-0000-0000-000000000106', 'admin-cultivation-milestones', 'GENERAL',
 'Hướng dẫn quản lý mốc canh tác',
 '["Tìm và lọc: nhập tên mốc vào ô tìm kiếm hoặc chọn Loại hoạt động (Gieo trồng, Tưới nước, Bón phân, Phun thuốc, Làm cỏ, Thu hoạch, Khác) để thu hẹp danh sách mốc canh tác.", "Xem nhanh phạm vi áp dụng: cột Loại nông sản và Tiêu chuẩn hiển thị Tất cả nếu mốc áp dụng cho mọi loại/tiêu chuẩn của lô, hoặc ghi tên cụ thể của loại nông sản/tiêu chuẩn.", "Thêm mốc mới: bấm nút Thêm mốc canh tác, khai báo Tên mốc, Loại hoạt động, Mô tả và Thời điểm dự kiến (số ngày sau khi gieo trồng), chọn phạm vi áp dụng rồi bấm Thêm mới.", "Chọn phạm vi áp dụng: ở mục Loại nông sản và Tiêu chuẩn, chọn Tất cả hoặc một loại nông sản / tiêu chuẩn cụ thể mà mốc sẽ áp dụng cho các lô.", "Quy định bắt buộc: bật công tắc Bắt buộc nếu mốc này là điều kiện bắt buộc để lô được đóng gói và kích hoạt tem; tắt nếu mốc chỉ mang tính khuyến khích.", "Sửa mốc: bấm biểu tượng cây bút ở cột Thao tác trên dòng cần chỉnh sửa, cập nhật thông tin rồi bấm Cập nhật. Lưu ý: tên mốc không được trùng trong cùng loại nông sản và tiêu chuẩn.", "Bấm Làm mới để tải lại danh sách mốc canh tác mới nhất."]',
 'Ví dụ: Mốc "Bón phân đợt 1" — Loại hoạt động: Bón phân; Thời điểm dự kiến: 15 ngày sau khi gieo trồng; Loại nông sản: Tất cả; Tiêu chuẩn: Tất cả; Bắt buộc: Bật.',
 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    steps = VALUES(steps),
    example_data = VALUES(example_data),
    updated_at = NOW();