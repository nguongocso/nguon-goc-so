-- ============================================================
-- V35: Seed & Update Help Content - Kiem nghiem chat luong
--
-- 1. Cap nhat noi dung huong dan cho man hinh tao yeu cau kiem nghiem
--    (screenKey: inspection-request-create) cho khop voi cac truong hien tai,
--    bo phan vi du.
-- 2. Bo sung noi dung huong dan cho man hinh ghi nhan ket qua kiem nghiem
--    (screenKey: inspection-result-record), bo phan vi du.
-- ============================================================

-- 1. Cap nhat huong dan tao yeu cau kiem nghiem
UPDATE help_content
SET steps = '["Kiểm tra thông tin lô sản xuất ở thẻ Thông tin lô sản xuất bên phải", "Chọn các chỉ tiêu phân tích cần kiểm nghiệm (hỗ trợ phân trang, tìm kiếm và lọc trạng thái Đã chọn / Chưa chọn)", "Nhập tên Đơn vị phòng Lab tiếp nhận và Ngày gửi mẫu (không được lớn hơn ngày hiện tại)", "Nhập Ghi chú bảo quản & Yêu cầu phân tích bổ sung nếu có", "Bấm Lưu bản nháp để lưu tạm thời, hoặc bấm Tạo yêu cầu kiểm nghiệm để gửi yêu cầu chính thức"]',
    example_data = NULL,
    updated_at = NOW()
WHERE screen_key = 'inspection-request-create';

-- 2. Bo sung huong dan ghi nhan ket qua kiem nghiem
INSERT INTO help_content
    (id, screen_key, role_code, title, steps, example_data, sort_order, created_at, updated_at)
VALUES
('00000000-0000-0000-0000-000000000047', 'inspection-result-record', 'GENERAL',
 'Hướng dẫn ghi nhận kết quả kiểm nghiệm',
 '["Kiểm tra thông tin yêu cầu kiểm nghiệm và danh sách chỉ tiêu phân tích cần nhập", "Với từng chỉ tiêu, chọn kết luận: Đạt chuẩn, Không đạt hoặc Không kiểm tra", "Nhập Giá trị đo thực tế, Phương pháp thử nghiệm và Ghi chú chi tiết cho từng chỉ tiêu", "Nhập Ngày nhận kết quả, Người thực hiện phân tích và Kết luận tổng quan của phòng Lab", "Đính kèm tệp tài liệu kết quả kiểm nghiệm (PDF hoặc ảnh scan, tối đa 5MB) nếu có", "Kiểm tra tiến độ nhập ở thanh tác vụ nổi bên dưới và bấm Lưu kết quả kiểm nghiệm để hoàn tất"]',
 NULL,
 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    steps = VALUES(steps),
    example_data = NULL,
    updated_at = NOW();
