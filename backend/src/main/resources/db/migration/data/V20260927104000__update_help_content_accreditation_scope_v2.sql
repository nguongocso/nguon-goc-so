-- ============================================================
-- V20260927104000: Cập nhật hướng dẫn quản lý phạm vi công nhận
-- (screenKey 'testing-unit-accreditation-scope'):
-- thẻ thông tin màu xanh lá chỉ còn Tên + Hết hạn (bỏ mã công nhận),
-- dòng đếm "Đã công nhận x/y chỉ tiêu", bảng cố định cột Ngưỡng/Công nhận.
-- ============================================================

UPDATE help_content
SET steps = '["Xem thông tin đơn vị: Thẻ thông tin màu xanh lá ở góc trên bên phải trang hiển thị tên đơn vị kiểm nghiệm và ngày hết hạn công nhận.", "Tra cứu chỉ tiêu: Nhập tên chỉ tiêu vào ô tìm kiếm rồi nhấn Enter, dùng bộ lọc đơn vị tính để lọc theo µg/kg, mg/kg, %..., nhấn \\"Làm mới\\" để tải lại danh mục chỉ tiêu; dòng \\"Đã công nhận x/y chỉ tiêu\\" cho biết số chỉ tiêu đang được công nhận trên tổng số chỉ tiêu đang hiển thị.", "Đọc bảng chỉ tiêu: Mỗi dòng gồm STT, Tên chỉ tiêu (kèm tiêu chuẩn tham chiếu), Ngưỡng tối đa (giá trị kèm đơn vị tính, ví dụ 5 µg/kg — là giới hạn lớn nhất được công nhận, kết quả kiểm nghiệm vượt ngưỡng này sẽ không đạt) và cột \\"Công nhận\\" ở giữa.", "Chọn chỉ tiêu công nhận: Bật công tắc ở cột \\"Công nhận\\" để đưa chỉ tiêu vào phạm vi, tắt công tắc để loại chỉ tiêu ra khỏi phạm vi.", "Lưu thay đổi: Nhấn nút \\"Lưu phạm vi công nhận\\" ở cuối trang; hệ thống sẽ thay thế toàn bộ phạm vi bằng tập chỉ tiêu đang chọn.", "Cảnh báo bỏ công nhận: Nếu bạn tắt công tắc của chỉ tiêu đã lưu trước đó, dòng cảnh báo màu hổ phách sẽ nhắc bạn kiểm tra lại trước khi nhấn lưu."]',
    updated_at = NOW()
WHERE screen_key = 'testing-unit-accreditation-scope'
  AND role_code = 'GENERAL';
