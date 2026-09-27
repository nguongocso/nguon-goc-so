-- ============================================================
-- V20260927100000: Seed Help Content cho màn hình quản lý đơn vị kiểm nghiệm
-- (screenKey 'testing-unit-management' - TestingUnitListPage)
-- ============================================================

INSERT INTO help_content
    (id, screen_key, role_code, title, steps, example_data, sort_order, created_at, updated_at)
VALUES
('00000000-0000-0000-0000-000000000121', 'testing-unit-management', 'GENERAL',
 'Hướng dẫn quản lý đơn vị kiểm nghiệm',
 '["Xem danh sách: Bảng liệt kê đơn vị kiểm nghiệm / phòng thí nghiệm gồm Tên đơn vị, Mã công nhận, Thông tin liên hệ, Ngày hết hạn và Trạng thái hoạt động.", "Tìm kiếm và lọc: Nhập tên hoặc mã công nhận vào ô tìm kiếm, chọn trạng thái (Tất cả / Đang hoạt động / Ngừng hoạt động) để thu hẹp kết quả, nhấn \\"Làm mới\\" để tải lại dữ liệu.", "Tạo đơn vị: Nhấn \\"Tạo đơn vị\\" ở góc trên bên phải, nhập đầy đủ thông tin đơn vị rồi lưu lại.", "Quản lý phạm vi công nhận: Tại cột \\"Hành động\\", nhấn biểu tượng phạm vi để thêm, sửa hoặc xóa phạm vi công nhận của đơn vị.", "Chỉnh sửa thông tin: Nhấn biểu tượng bút chì tại cột \\"Hành động\\" để cập nhật lại thông tin đơn vị.", "Ngừng hoạt động: Nhấn biểu tượng vô hiệu hoá rồi xác nhận trong hộp thoại, đơn vị sẽ chuyển sang trạng thái \\"Ngừng hoạt động\\" và không còn xuất hiện khi tạo yêu cầu kiểm nghiệm."]',
 NULL, 0, NOW(), NOW());
