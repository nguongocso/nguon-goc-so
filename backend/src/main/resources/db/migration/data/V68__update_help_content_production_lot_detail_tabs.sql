-- ============================================================
-- V68: Update Help Content - Production Lot Detail Tab Order
--
-- Cập nhật nội dung hướng dẫn cho màn hình Chi tiết Lô sản xuất
-- (production-lot-detail) để phản ánh thứ tự tab mới sau khi sắp xếp
-- lại theo luồng nghiệp vụ:
--
-- 1. Nhật ký canh tác
-- 2. Kiểm nghiệm
-- 3. Chứng nhận
-- 4. Lô hàng & Mã QR
-- ============================================================

-- Cập nhật nội dung hướng dẫn chung (GENERAL)
UPDATE help_content
SET
    title = 'Hướng dẫn thao tác trang chi tiết lô sản xuất',
    steps = '[
      "Xem tổng quan lô: thông tin cơ bản, trạng thái, diện tích",
      "Theo dõi nhật ký canh tác (tab 1) để xem các hoạt động sản xuất",
      "Kiểm tra kết quả kiểm nghiệm (tab 2) để đánh giá chất lượng lô",
      "Xem thông tin chứng nhận (tab 3) gắn với lô sản xuất",
      "Theo dõi lô hàng và mã QR (tab 4) đã tạo từ lô sản xuất"
    ]',
    updated_at = NOW()
WHERE screen_key = 'production-lot-detail'
  AND role_code = 'GENERAL';

-- Cập nhật nội dung hướng dẫn cho VT-03 (Người ghi sự kiện)
UPDATE help_content
SET
    title = 'Hướng dẫn chi tiết lô cho người ghi sự kiện',
    steps = '[
      "Xem thông tin lô và trạng thái hiện tại",
      "Bấm Ghi nhật ký (tab 1) để thêm hoạt động canh tác",
      "Kiểm tra kết quả kiểm nghiệm (tab 2) nếu lô đã được kiểm nghiệm",
      "Xem chứng nhận (tab 3) đã gắn với lô",
      "Xem lô hàng và mã QR (tab 4) đã tạo từ lô sản xuất"
    ]',
    updated_at = NOW()
WHERE screen_key = 'production-lot-detail'
  AND role_code = 'VT-03';
