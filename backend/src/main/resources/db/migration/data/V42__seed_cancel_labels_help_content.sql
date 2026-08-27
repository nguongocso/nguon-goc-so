-- ============================================================
-- V42: Seed Help Content for Cancel Labels & Cancellation History (NCL-04-CN-006)
-- ============================================================

INSERT IGNORE INTO help_content
    (id, screen_key, role_code, title, steps, example_data, sort_order, created_at, updated_at)
VALUES
-- 1. Hủy tem in hỏng (cancel-labels)
('00000000-0000-0000-0000-000000000047', 'cancel-labels', 'GENERAL',
 'Hướng dẫn đánh dấu hủy tem in hỏng & hoàn hạn mức',
 '["Gom danh sách tem in mờ, nhòe QR, lệch viền hoặc bị rách trong khi dán. Chỉ tem ở trạng thái Chưa kích hoạt (INACTIVE) mới được hủy.", "Chọn phương thức nhập mã: Chọn Theo khoảng mã (Range) nếu dải tem liên tiếp, hoặc Chọn Nhập từng mã lẻ (Single) và dùng súng quét mã vạch USB / dán danh sách mã.", "Chọn lý do tiêu hủy: Chọn lý do phù hợp (In mờ, in lệch, bong tróc). Nếu chọn Lý do khác, điền ghi chú giải trình chi tiết tối thiểu 10 ký tự.", "Xác nhận & Hoàn hạn mức: Bấm Xác nhận hủy & Hoàn hạn mức. Mã tem đổi sang CANCELLED và hạn mức dải mã của HTX được tự động hoàn trả ngay lập tức."]',
 'Ví dụ: Hủy khoảng mã HTX01-00000010 đến HTX01-00000025 do kẹt giấy máy in lô #2.', 0, NOW(), NOW()),

-- 2. Lịch sử hủy tem (label-cancellation-history)
('00000000-0000-0000-0000-000000000048', 'label-cancellation-history', 'GENERAL',
 'Hướng dẫn tra cứu lịch sử hủy tem in hỏng',
 '["Xem thẻ tổng quan thống kê: Tổng số tem đã hủy, số đợt thao tác và hạn mức dải mã đã hoàn trả.", "Sử dụng công cụ tìm kiếm theo mã tem, ghi chú giải trình hoặc tài khoản người thực hiện.", "Lọc danh sách theo phương thức (Khoảng mã / Mã lẻ) hoặc theo lý do tiêu hủy.", "Sử dụng thanh phân trang ở chân bảng để chuyển giữa các trang (tối đa 10 đợt/trang)."]',
 'Ví dụ: Lọc lý do "In hỏng/mờ/nhòe QR" để thống kê tổng số tem bị hỏng do mực in.', 0, NOW(), NOW());
