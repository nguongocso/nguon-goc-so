-- ============================================================
-- V20260927103000: Cập nhật nội dung hướng dẫn quản lý phạm vi công nhận
-- (screenKey 'testing-unit-accreditation-scope'):
-- thẻ thông tin đơn vị góc phải, bộ lọc đơn vị tính, bảng 4 cột
-- (ngưỡng tối đa gộp đơn vị tính), cột Công nhận chỉ còn công tắc.
-- ============================================================

UPDATE help_content
SET steps = '["Xem thông tin đơn vị: Thẻ thông tin ở góc trên bên phải trang hiển thị tên đơn vị kiểm nghiệm, mã công nhận và ngày hết hạn công nhận.", "Tra cứu chỉ tiêu: Nhập tên chỉ tiêu vào ô tìm kiếm rồi nhấn Enter, dùng bộ lọc đơn vị tính để lọc theo µg/kg, mg/kg, %..., nhấn \\"Làm mới\\" để tải lại danh mục chỉ tiêu.", "Đọc bảng chỉ tiêu: Mỗi dòng gồm STT, Tên chỉ tiêu (kèm tiêu chuẩn tham chiếu), Ngưỡng tối đa (giá trị kèm đơn vị tính, ví dụ 5 µg/kg — là giới hạn lớn nhất được công nhận, kết quả kiểm nghiệm vượt ngưỡng này sẽ không đạt) và cột \\"Công nhận\\" ở giữa.", "Chọn chỉ tiêu công nhận: Bật công tắc ở cột \\"Công nhận\\" để đưa chỉ tiêu vào phạm vi, tắt công tắc để loại chỉ tiêu ra khỏi phạm vi.", "Lưu thay đổi: Nhấn nút \\"Lưu phạm vi công nhận\\" ở cuối trang; hệ thống sẽ thay thế toàn bộ phạm vi bằng tập chỉ tiêu đang chọn.", "Cảnh báo bỏ công nhận: Nếu bạn tắt công tắc của chỉ tiêu đã lưu trước đó, dòng cảnh báo màu hổ phách sẽ nhắc bạn kiểm tra lại trước khi nhấn lưu."]',
    updated_at = NOW()
WHERE screen_key = 'testing-unit-accreditation-scope'
  AND role_code = 'GENERAL';
