-- ============================================================
-- V38: Seed Help Content for Inspection Criteria Management
-- (NCL-09-CN-009)
--
-- Nội dung hướng dẫn quản lý danh mục chỉ tiêu kiểm nghiệm
-- cho screenKey 'admin-inspection-criteria'. Frontend đã gắn
-- HelpButton với screenKey này từ đầu story; trước đó chưa có
-- dữ liệu nên drawer hiển thị "Chưa có hướng dẫn cho màn hình này".
-- ============================================================

INSERT INTO help_content
    (id, screen_key, role_code, title, steps, example_data, sort_order, created_at, updated_at)
VALUES
('00000000-0000-0000-0000-000000000049', 'admin-inspection-criteria', 'GENERAL',
 'Hướng dẫn quản lý chỉ tiêu kiểm nghiệm',
 '["Tìm kiếm và lọc: nhập tên chỉ tiêu hoặc tên tiêu chuẩn vào ô tìm kiếm, kết hợp bộ lọc Trạng thái (Tất cả / Đang hoạt động / Ngừng sử dụng); bấm Làm mới để tải lại danh sách.", "Thêm chỉ tiêu: bấm nút Thêm chỉ tiêu, khai báo Tên chỉ tiêu, Đơn vị đo (ví dụ mg/kg), Ngưỡng tối đa và Tiêu chuẩn tham chiếu rồi Lưu. Kết quả kiểm nghiệm vượt ngưỡng tối đa được coi là không đạt.", "Sửa chỉ tiêu: bấm biểu tượng cây bút ở cột Thao tác trên dòng cần chỉnh sửa.", "Ngừng sử dụng / Kích hoạt lại: bấm biểu tượng con mắt ở cột Thao tác. Chỉ tiêu ngừng sử dụng không còn áp dụng cho yêu cầu kiểm nghiệm mới nhưng vẫn giữ nguyên lịch sử dữ liệu.", "Xóa: chỉ xóa được chỉ tiêu chưa từng được yêu cầu kiểm nghiệm nào tham chiếu; chỉ tiêu đã được tham chiếu chỉ nên ngừng sử dụng để bảo toàn dữ liệu.", "Dùng Trang trước / Trang sau để di chuyển giữa các trang khi danh sách dài."]',
 'Ví dụ: Tên chỉ tiêu: Dư lượng thuốc BVTV nhóm Lân hữu cơ; Đơn vị đo: mg/kg; Ngưỡng tối đa: 0.5; Tiêu chuẩn tham chiếu: chọn từ danh mục Tiêu chuẩn chất lượng.',
 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    steps = VALUES(steps),
    example_data = VALUES(example_data),
    updated_at = NOW();