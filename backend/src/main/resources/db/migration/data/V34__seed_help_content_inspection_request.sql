-- ============================================================
-- V33: Seed Help Content - Trang tạo yêu cầu kiểm nghiệm (NCL-666)
--
-- Bổ sung nội dung hướng dẫn cho màn hình tạo yêu cầu kiểm nghiệm
-- (screenKey: inspection-request-create) trước đây chưa có dữ liệu,
-- drawer hướng dẫn hiển thị "Chưa có hướng dẫn cho màn hình này".
-- ============================================================

INSERT IGNORE INTO help_content
    (id, screen_key, role_code, title, steps, example_data, sort_order, created_at, updated_at)
VALUES
('00000000-0000-0000-0000-000000000046', 'inspection-request-create', 'GENERAL',
 'Hướng dẫn tạo yêu cầu kiểm nghiệm',
 '["Đối soát thông tin lô sản xuất ở cột bên phải trước khi gửi mẫu", "Chọn bộ chỉ tiêu phân tích theo tiêu chuẩn áp dụng cho lô (mặc định đã chọn tất cả, có thể tìm kiếm và lọc)", "Nhập tên đơn vị phòng Lab tiếp nhận và ngày gửi mẫu (không được lớn hơn ngày hiện tại)", "Chọn phương thức giao nhận mẫu và ghi chú bảo quản, yêu cầu phân tích bổ sung nếu cần", "Đính kèm biên bản lấy mẫu hoặc ảnh niêm phong mẫu (tối đa 10MB/tệp)", "Bấm Lưu bản nháp để tạm lưu chưa gửi, hoặc Tạo yêu cầu kiểm nghiệm để gửi chính thức"]',
 'Ví dụ: Chọn tất cả chỉ tiêu theo tiêu chuẩn áp dụng, gửi mẫu tới "Phòng thí nghiệm Trung tâm", chọn phương thức Phòng Lab lấy mẫu tại vườn.', 0, '2026-08-25 00:00:00', '2026-08-25 00:00:00');
