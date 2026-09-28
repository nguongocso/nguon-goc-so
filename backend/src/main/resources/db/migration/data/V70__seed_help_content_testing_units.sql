-- ============================================================
-- V70: Seed Help Content - Quản lý danh mục đơn vị kiểm nghiệm
--
-- Bổ sung nội dung hướng dẫn cho 3 màn hình của chức năng Đơn vị kiểm nghiệm:
--   - testing-unit-management           (/admin/testing-units)
--   - testing-unit-form                 (/admin/testing-units/create, /:id/edit)
--   - testing-unit-accreditation-scope   (/admin/testing-units/:id/scopes)
-- Trước đó chưa có dữ liệu nên drawer hướng dẫn hiển thị
-- "Chưa có hướng dẫn cho màn hình này".
-- ============================================================

INSERT INTO help_content
    (id, screen_key, role_code, title, steps, example_data, sort_order, created_at, updated_at)
VALUES
('00000000-0000-0000-0000-000000000121', 'testing-unit-management', 'GENERAL',
 'Hướng dẫn quản lý danh mục đơn vị kiểm nghiệm',
 '["Đọc danh sách: bảng hiển thị Tên đơn vị, Mã công nhận, Thông tin liên hệ, Ngày hết hạn và Trạng thái của từng phòng thí nghiệm / đơn vị kiểm nghiệm trong danh mục dùng chung.", "Tìm và lọc: nhập tên hoặc mã công nhận vào ô tìm kiếm, hoặc chọn Trạng thái (Tất cả, Đang hoạt động, Ngừng hoạt động) để thu hẹp danh sách. Bấm Làm mới để tải lại dữ liệu mới nhất.", "Theo dõi hạn công nhận: cột Ngày hết hạn hiển thị ngày hết hạn công nhận của đơn vị. Nếu ngày này nhỏ hơn ngày hiện tại thì hiển thị kèm chữ (đã hết hạn) và chữ được tô đỏ.", "Tạo đơn vị mới: bấm nút Tạo đơn vị, khai báo Tên đơn vị, Mã công nhận, Thông tin liên hệ và Ngày hết hạn công nhận rồi bấm Lưu.", "Sửa thông tin: bấm biểu tượng cây bút ở cột Hành động trên dòng cần chỉnh sửa, cập nhật thông tin rồi bấm Cập nhật.", "Ngừng hoạt động đơn vị: bấm biểu tượng con mắt gạch ở cột Hành động và xác nhận trong hộp thoại. Đơn vị đã ngừng hoạt động sẽ không còn xuất hiện trong danh sách lựa chọn khi tạo yêu cầu kiểm nghiệm. Nếu cần dùng lại, liên hệ Quản trị viên hệ thống.", "Khai báo phạm vi công nhận: bấm nút Phạm vi ở cột Hành động để chọn các chỉ tiêu mà đơn vị được phép thực hiện kiểm nghiệm."]',
 'Ví dụ: Đơn vị "Trung tâm mới giới" — Mã công nhận: VSHJ; Ngày hết hạn: 2026-10-02; Trạng thái: Đang hoạt động; Phạm vi công nhận: Aflatoxin B1, Dư lượng thuốc BVTV.',
 0, NOW(), NOW()),
('00000000-0000-0000-0000-000000000122', 'testing-unit-form', 'GENERAL',
 'Hướng dẫn tạo và cập nhật đơn vị kiểm nghiệm',
 '["Khai báo thông tin bắt buộc: nhập Tên đơn vị (tối đa 255 ký tự) và Mã công nhận (tối đa 100 ký tự) do cơ quan công nhận cấp. Hai trường này không được để trống.", "Nhập Thông tin liên hệ (tối đa 500 ký tự) như địa chỉ, số điện thoại hoặc email để liên hệ khi cần.", "Chọn Ngày hết hạn công nhận: ngày công nhận của đơn vị hết hiệu lực. Không được chọn ngày ở quá khứ; để trống nếu chưa xác định.", "Lưu thay đổi: bấm nút Lưu để tạo mới hoặc cập nhật. Hệ thống sẽ thông báo kết quả và đưa bạn trở lại danh sách đơn vị kiểm nghiệm.", "Sau khi tạo đơn vị, chuyển sang màn hình Phạm vi công nhận để khai báo các chỉ tiêu mà đơn vị được phép thực hiện kiểm nghiệm."]',
 'Ví dụ: Tên đơn vị: Trung tâm mới giới; Mã công nhận: VSHJ; Thông tin liên hệ: 12 Lê Lợi, Q.1, TP.HCM; Ngày hết hạn công nhận: 2026-10-02.',
 0, NOW(), NOW()),
('00000000-0000-0000-0000-000000000123', 'testing-unit-accreditation-scope', 'GENERAL',
 'Hướng dẫn khai báo phạm vi công nhận của đơn vị kiểm nghiệm',
 '["Đọc thông tin đơn vị: đầu trang hiển thị Tên đơn vị, Mã công nhận và Ngày hết hạn để đối chiếu trước khi khai báo.", "Ý nghĩa phạm vi công nhận: đây là tập chỉ tiêu mà đơn vị được phép thực hiện kiểm nghiệm. Khi tạo yêu cầu kiểm nghiệm có chọn chỉ tiêu nằm ngoài phạm vi này, hệ thống sẽ cảnh báo.", "Tìm nhanh chỉ tiêu: nhập tên chỉ tiêu hoặc mã chỉ tiêu vào ô tìm kiếm, hoặc lọc theo tiêu chuẩn chất lượng và trạng thái.", "Chọn chỉ tiêu được công nhận: tick vào ô chọn ở từng dòng chỉ tiêu. Thẻ đếm ở góc phải cho biết đã chọn bao nhiêu trên tổng số chỉ tiêu.", "Lưu phạm vi: bấm nút Lưu để lưu danh sách chỉ tiêu đã chọn. Thay đổi chỉ có hiệu lực sau khi lưu thành công.", "Gỡ bỏ phạm vi: bỏ tick ở các chỉ tiêu không còn được công nhận rồi bấm Lưu."]',
 'Ví dụ: Đơn vị "Trung tâm mới giới" (mã công nhận VSHJ) được công nhận 3/18 chỉ tiêu: Aflatoxin B1, Dư lượng thuốc BVTV, Hàm lượng kim loại nặng.',
 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    steps = VALUES(steps),
    example_data = VALUES(example_data),
    updated_at = NOW();
