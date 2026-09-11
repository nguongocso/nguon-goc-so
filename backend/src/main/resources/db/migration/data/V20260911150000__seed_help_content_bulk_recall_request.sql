-- ============================================================
-- V20260911150000: Seed Help Content - Yêu cầu thu hồi theo phạm vi ảnh hưởng (NCL-08-CN-011, NCL-08-CN-012)
-- ============================================================

INSERT INTO help_content
    (id, screen_key, role_code, title, steps, example_data, sort_order, created_at, updated_at)
VALUES
('00000000-0000-0000-0000-000000000120', 'bulk-recall-request-list', 'GENERAL',
 'Yêu cầu thu hồi theo phạm vi ảnh hưởng',
 '["1. Xem danh sách các yêu cầu thu hồi hàng loạt theo phạm vi ảnh hưởng của hợp tác xã.", "2. Sử dụng thanh tìm kiếm hoặc lọc trạng thái (Chờ duyệt, Đã duyệt, Từ chối, Đã xử lý) để tra cứu yêu cầu cần xử lý.", "3. Tại cột \\"Thao tác\\", với yêu cầu \\"Chờ duyệt\\": Quản lý HTX có thể nhấn icon Phê duyệt (dấu tích xanh) hoặc Từ chối (dấu X đỏ) và nhập lý do.", "4. Tại cột \\"Thao tác\\", với yêu cầu \\"Đã duyệt\\": Quản lý HTX nhấn icon Kết thúc vụ việc (sổ kiểm tra) để ghi nhận kết quả xử lý các lô, biện pháp khắc phục và đính kèm tối đa 5 tệp biên bản (.pdf, .docx).", "5. Tại cột \\"Chi tiết\\", chọn biểu tượng Chi tiết (con mắt) để xem toàn bộ thông tin yêu cầu, danh sách lô hàng ảnh hưởng, tiến trình và mở xem tệp biên bản đính kèm trên tab mới."]',
 'Ví dụ: Lọc trạng thái "Đã duyệt" -> Bấm icon Kết thúc vụ việc để đóng hồ sơ thu hồi và tải lên tệp biên bản nghiệm thu.',
 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    steps = VALUES(steps),
    example_data = VALUES(example_data),
    updated_at = NOW();
