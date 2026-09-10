-- ============================================================
-- V69: Seed Help Content - Ghi nhận nhập/xuất kho HTX (NCL-05-CN-011)
--
-- Bổ sung nội dung hướng dẫn trợ giúp người dùng cho 2 màn hình:
--   - 'coop-warehouse-entry' (CreateCoopWarehouseEntryPage)
--   - 'coop-warehouse-exit'  (CreateCoopWarehouseExitPage)
-- ============================================================

INSERT INTO help_content
    (id, screen_key, role_code, title, steps, example_data, sort_order, created_at, updated_at)
VALUES
('00000000-0000-0000-0000-000000000107', 'coop-warehouse-entry', 'GENERAL',
 'Hướng dẫn ghi nhận nhập kho HTX',
 '["1. Kiểm tra danh sách lô hàng: Xem lại thông tin các lô hàng được chọn để ghi nhận nhập kho. Lưu ý: Lô hàng nhập mới phải chưa ở trong kho HTX.", "2. Khai báo thông tin kho: Nhập Tên kho lưu trữ (VD: Kho lạnh HTX Nông nghiệp Số 1) và Chọn Thời điểm nhập kho chính xác.", "3. Điều kiện bảo quản: Khai báo nhiệt độ, độ ẩm hoặc môi trường lưu trữ (VD: Nhiệt độ 4°C - 8°C, Độ ẩm 85%).", "4. Vị trí bản đồ (GPS): Hệ thống sẽ tự động lấy vị trí hiện tại của thiết bị qua GPS, bạn cũng có thể click trực tiếp trên bản đồ để điều chỉnh vị trí kho.", "5. Ghi chú bổ sung: Điền thêm các thông tin diễn giải hoặc ghi chú lưu ý khi tiếp nhận lô hàng.", "6. Hoàn tất: Bấm Ghi nhận nhập kho để lưu sự kiện và kích hoạt tính thời gian lưu kho cho lô hàng."]',
 'Ví dụ: Tên kho: Kho lạnh HTX Nông nghiệp Số 1 | Điều kiện: Nhiệt độ 4°C - 8°C, Độ ẩm 85% | Vị trí: Tự động xác định qua GPS.',
 0, NOW(), NOW()),

('00000000-0000-0000-0000-000000000108', 'coop-warehouse-exit', 'GENERAL',
 'Hướng dẫn ghi nhận xuất kho HTX',
 '["1. Kiểm tra danh sách lô hàng: Danh sách các lô hàng xuất kho. Tất cả lô hàng phải đang ở trong kho HTX (đã có sự kiện nhập kho trước đó).", "2. Khai báo thời điểm & điểm đến: Chọn Thời điểm xuất kho và Nhập điểm đến / Nơi nhận hàng (VD: Siêu thị WinMart Cầu Giấy, Đối tác thu mua ABC).", "3. Cảnh báo quá hạn lưu kho: Nếu lô hàng lưu kho vượt quá thời gian tối đa cho phép theo cấu hình danh mục sản phẩm, hệ thống sẽ cảnh báo thời gian lưu kho.", "4. Vị trí bản đồ (GPS): Tọa độ địa lý sẽ tự động cập nhật từ vị trí hiện tại của bạn hoặc click chọn trực tiếp trên bản đồ.", "5. Ghi chú bổ sung: Nhập thông tin niêm phong, mã vận đơn hoặc phương tiện vận chuyển nếu có.", "6. Hoàn tất: Bấm Ghi nhận xuất kho để kết thúc giai đoạn lưu kho và chuyển lô hàng sang công đoạn tiếp theo."]',
 'Ví dụ: Điểm đến: Siêu thị Co.opmart Hà Nội | Thời điểm: Hiện tại | Vị trí: Tự động lấy từ vị trí GPS thiết bị.',
 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    steps = VALUES(steps),
    example_data = VALUES(example_data),
    updated_at = NOW();
