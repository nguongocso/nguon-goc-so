-- ============================================================
-- V20260927102000: Sửa JSON steps cho help_content 'testing-unit-management'
-- Bản seed V20260927100000 đã chạy với escape thiếu (\" thay vì \\") nên
-- MySQL ghi nhẫn trích dẫn thô làm steps thành JSON không hợp lệ.
-- Migration đã apply nên không được sửa trực tiếp — tạo bản sửa mới (idempotent).
-- ============================================================

UPDATE help_content
SET steps = '["Xem danh sách: Bảng liệt kê đơn vị kiểm nghiệm / phòng thí nghiệm gồm Tên đơn vị, Mã công nhận, Thông tin liên hệ, Ngày hết hạn và Trạng thái hoạt động.", "Tìm kiếm và lọc: Nhập tên hoặc mã công nhận vào ô tìm kiếm, chọn trạng thái (Tất cả / Đang hoạt động / Ngừng hoạt động) để thu hẹp kết quả, nhấn \\"Làm mới\\" để tải lại dữ liệu.", "Tạo đơn vị: Nhấn \\"Tạo đơn vị\\" ở góc trên bên phải, nhập đầy đủ thông tin đơn vị rồi lưu lại.", "Quản lý phạm vi công nhận: Tại cột \\"Hành động\\", nhấn biểu tượng phạm vi để thêm, sửa hoặc xóa phạm vi công nhận của đơn vị.", "Chỉnh sửa thông tin: Nhấn biểu tượng bút chì tại cột \\"Hành động\\" để cập nhật lại thông tin đơn vị.", "Ngừng hoạt động: Nhấn biểu tượng vô hiệu hoá rồi xác nhận trong hộp thoại, đơn vị sẽ chuyển sang trạng thái \\"Ngừng hoạt động\\" và không còn xuất hiện khi tạo yêu cầu kiểm nghiệm."]',
    updated_at = NOW()
WHERE screen_key = 'testing-unit-management'
  AND role_code = 'GENERAL'
  AND NOT JSON_VALID(steps);
