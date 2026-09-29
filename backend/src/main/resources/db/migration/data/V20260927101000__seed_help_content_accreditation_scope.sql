-- ============================================================
-- V20260927101000: Seed Help Content cho màn hình quản lý phạm vi công nhận
-- (screenKey 'testing-unit-accreditation-scope' - TestingUnitScopeManagerPage)
-- ============================================================

INSERT INTO help_content
    (id, screen_key, role_code, title, steps, example_data, sort_order, created_at, updated_at)
VALUES
('00000000-0000-0000-0000-000000000122', 'testing-unit-accreditation-scope', 'GENERAL',
 'Hướng dẫn quản lý phạm vi công nhận',
 '["Xem thông tin đơn vị: Tiêu đề hiển thị tên đơn vị, mã công nhận và ngày hết hạn của đơn vị kiểm nghiệm đang quản lý.", "Tra cứu chỉ tiêu: Nhập tên chỉ tiêu vào ô tìm kiếm rồi nhấn Enter, dùng bộ lọc (Tất cả / Đã công nhận / Chưa công nhận) và nút \\"Làm mới\\" để tải lại danh mục chỉ tiêu.", "Đọc bảng chỉ tiêu: Mỗi dòng gồm STT, Tên chỉ tiêu (kèm tiêu chuẩn tham chiếu), Đơn vị tính, Ngưỡng tối đa và cột \\"Công nhận\\" căn giữa.", "Chọn chỉ tiêu công nhận: Bật công tắc tại cột \\"Công nhận\\" để đưa chỉ tiêu vào phạm vi, tắt công tắc để loại chỉ tiêu ra khỏi phạm vi.", "Lưu thay đổi: Nhấn nút \\"Lưu phạm vi công nhận\\" ở cuối trang; hệ thống sẽ thay thế toàn bộ phạm vi bằng tập chỉ tiêu đang chọn.", "Cảnh báo bỏ công nhận: Nếu bạn tắt công tắc của chỉ tiêu đã lưu trước đó, hệ thống hiển thị cảnh báo màu hổ phách phía trên nút lưu để bạn kiểm tra lại trước khi xác nhận."]',
 NULL, 0, NOW(), NOW());
