-- ============================================================
-- V29: Seed Help Content (NCL-01-CN-006 - In-App User Guidance)
--
-- Mỗi màn hình có 1 nội dung chung (role_code = 'GENERAL').
-- Một số màn hình có thêm nội dung riêng theo vai trò (role_code = 'VT-xx')
-- khi quy trình của từng vai trò khác nhau đáng kể.
--
-- Quy tắc hiển thị (HelpServiceImpl):
--   1. Ưu tiên nội dung khớp screenKey + roleCode
--   2. Nếu không có -> nội dung chung GENERAL
--   3. Nếu vẫn không có -> frontend hiển thị thông báo mặc định
-- ============================================================

INSERT IGNORE INTO help_content
    (id, screen_key, role_code, title, steps, example_data, sort_order, created_at, updated_at)
VALUES
-- ============================================================
-- 1. Dashboard
-- ============================================================
('00000000-0000-0000-0000-000000000001', 'dashboard', 'GENERAL',
 'Hướng dẫn sử dụng bảng điều khiển',
 '["Xem các chỉ số tổng quan về lô sản xuất, sự kiện chuỗi cung ứng và cảnh báo", "Chọn bộ lọc thời gian (ngày/tuần/tháng) để xem số liệu phù hợp", "Nhấn vào thẻ chỉ số để truy cập nhanh trang chi tiết", "Dùng các nút hành động nhanh để tạo lô, ghi sự kiện hoặc tra cứu mã"]',
 'Ví dụ: Xem "Tổng số lô đang sản xuất" để theo dõi tiến độ trong tuần.', 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 2. Danh sách lô sản xuất
-- ============================================================
('00000000-0000-0000-0000-000000000002', 'production-lot-list', 'GENERAL',
 'Hướng dẫn danh sách lô sản xuất',
 '["Dùng thanh tìm kiếm và bộ lọc (trạng thái, khu vực, vụ mùa) để thu hẹp danh sách", "Nhấn vào tên lô để mở trang chi tiết", "Chọn hành động nhanh: chỉnh sửa, gửi duyệt hoặc xem nhật ký"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),
('00000000-0000-0000-0000-000000000101', 'production-lot-list', 'VT-02',
 'Hướng dẫn duyệt lô sản xuất',
 '["Lọc danh sách lô theo trạng thái chờ duyệt", "Nhấn vào lô để xem chi tiết trước khi quyết định", "Duyệt hoặc từ chối lô từ màn hình chỉnh sửa", "Theo dõi trạng thái lô sau khi duyệt"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),
('00000000-0000-0000-0000-000000000102', 'production-lot-list', 'VT-03',
 'Hướng dẫn theo dõi lô sản xuất cho người ghi sự kiện',
 '["Lọc danh sách lô theo trạng thái", "Mở chi tiết lô để ghi nhật ký canh tác hoặc tạo sự kiện", "Gửi lô cho quản lý duyệt khi hoàn tất sản xuất"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 3. Tạo lô sản xuất
-- ============================================================
('00000000-0000-0000-0000-000000000003', 'production-lot-create', 'GENERAL',
 'Hướng dẫn tạo lô sản xuất',
 '["Chọn khu vực canh tác và danh mục sản phẩm phù hợp", "Nhập tên lô, vụ mùa, diện tích và ngày bắt đầu", "Điền thông tin giống cây trồng nếu có", "Bấm Lưu để tạo lô"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 4. Chỉnh sửa lô sản xuất
-- ============================================================
('00000000-0000-0000-0000-000000000004', 'production-lot-edit', 'GENERAL',
 'Hướng dẫn chỉnh sửa lô sản xuất',
 '["Cập nhật thông tin lô (tên, diện tích, vụ mùa)", "Đính kèm ghi chú thay đổi để lưu vết", "Bấm Lưu thay đổi", "Với lô mới, dùng nút Duyệt để kích hoạt lô"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 5. Chi tiết lô sản xuất
-- ============================================================
('00000000-0000-0000-0000-000000000005', 'production-lot-detail', 'GENERAL',
 'Hướng dẫn xem chi tiết lô sản xuất',
 '["Xem tổng quan lô: thông tin cơ bản, trạng thái, diện tích", "Cuộn xem các lô hàng (shipments) trực thuộc", "Xem nhật ký canh tác và các sự kiện của lô", "Theo dõi mã truy xuất đã cấp cho lô"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),
('00000000-0000-0000-0000-000000000103', 'production-lot-detail', 'VT-03',
 'Hướng dẫn chi tiết lô cho người ghi sự kiện',
 '["Xem thông tin lô và trạng thái hiện tại", "Bấm Ghi nhật ký để thêm hoạt động canh tác", "Ghi sự kiện sơ chế/đóng gói nếu lô đã thu hoạch", "Tạo yêu cầu thu hồi nếu phát hiện bất thường"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 6. Nhập khẩu lô sản xuất
-- ============================================================
('00000000-0000-0000-0000-000000000006', 'production-lot-import', 'GENERAL',
 'Hướng dẫn nhập khẩu lô sản xuất',
 '["Tải file mẫu (template) nhập khẩu lô", "Điền dữ liệu các lô theo đúng định dạng cột", "Tải file lên và xem kết quả kiểm tra", "Sửa các dòng lỗi theo thông báo rồi tải lại"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 7. Ghi nhật ký canh tác
-- ============================================================
('00000000-0000-0000-0000-000000000007', 'farm-log-create', 'GENERAL',
 'Hướng dẫn ghi nhật ký canh tác',
 '["Chọn lô sản xuất phù hợp từ danh sách", "Nhập hoạt động canh tác (bón phân, tưới, phòng trừ...)", "Chụp hoặc tải lên ảnh minh chứng", "Bấm Lưu để gửi nhật ký cho quản lý duyệt"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 8. Lịch sử nhật ký canh tác
-- ============================================================
('00000000-0000-0000-0000-000000000008', 'farm-log-history', 'GENERAL',
 'Hướng dẫn xem lịch sử nhật ký canh tác',
 '["Chọn lô sản xuất để xem toàn bộ nhật ký canh tác", "Xem chi tiết từng nhật ký kèm ảnh minh chứng", "Duyệt nhật ký hợp lệ hoặc từ chối kèm lý do", "Theo dõi trạng thái duyệt của từng nhật ký"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 9. Ghi sự kiện sơ chế
-- ============================================================
('00000000-0000-0000-0000-000000000009', 'preprocessing-event-create', 'GENERAL',
 'Hướng dẫn ghi sự kiện sơ chế',
 '["Chọn lô sản xuất đã thu hoạch", "Ghi loại sơ chế (rửa, phân loại, bảo quản...) và khối lượng", "Nhập thời gian, địa điểm và người thực hiện", "Bấm Lưu để ghi nhận sự kiện vào chuỗi"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 10. Chỉnh sửa sự kiện sơ chế
-- ============================================================
('00000000-0000-0000-0000-000000000010', 'preprocessing-event-correct', 'GENERAL',
 'Hướng dẫn chỉnh sửa sự kiện sơ chế',
 '["Mở sự kiện sơ chế cần chỉnh sửa", "Chỉnh lại thông tin sai", "Nhập lý do chỉnh sửa (bắt buộc)", "Bấm Lưu — hệ thống ghi lại bản sửa và lý do"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 11. Ghi sự kiện đóng gói
-- ============================================================
('00000000-0000-0000-0000-000000000011', 'packaging-event-create', 'GENERAL',
 'Hướng dẫn ghi sự kiện đóng gói',
 '["Chọn lô sản xuất đã sơ chế", "Nhập số bao bì, khối lượng đóng gói", "Kiểm tra dải mã truy xuất hệ thống tự sinh", "Bấm Lưu để gán mã cho từng bao bì"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 12. Chỉnh sửa sự kiện đóng gói
-- ============================================================
('00000000-0000-0000-0000-000000000012', 'packaging-event-correct', 'GENERAL',
 'Hướng dẫn chỉnh sửa sự kiện đóng gói',
 '["Mở sự kiện đóng gói cần chỉnh sửa", "Sửa số liệu bao bì/khối lượng", "Nhập lý do chỉnh sửa (bắt buộc)", "Bấm Lưu — lịch sử mã truy xuất được giữ lại"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 13. Ghi sự kiện vận chuyển
-- ============================================================
('00000000-0000-0000-0000-000000000013', 'transport-event-record', 'GENERAL',
 'Hướng dẫn ghi sự kiện vận chuyển',
 '["Quét mã truy xuất trên bao bì/lô hàng", "Chọn phương tiện và người vận chuyển", "Nhập điểm đi, điểm đến, thời gian", "Bấm Lưu để ghi nhận chặng vận chuyển"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 14. Quét sự kiện nhanh
-- ============================================================
('00000000-0000-0000-0000-000000000014', 'scan-quick-event', 'GENERAL',
 'Hướng dẫn quét sự kiện nhanh',
 '["Quét mã truy xuất cần ghi sự kiện", "Chọn loại sự kiện nhanh (nhập kho, xuất kho, hư hỏng...)", "Điền thông tin bổ sung nếu cần", "Bấm Lưu — sự kiện được thêm ngay vào chuỗi"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 15. Sự kiện thu mua
-- ============================================================
('00000000-0000-0000-0000-000000000015', 'procurement-event', 'GENERAL',
 'Hướng dẫn ghi sự kiện thu mua',
 '["Xác nhận lô hàng cần thu mua từ danh sách", "Nhập thông tin thu mua: khối lượng, đơn giá", "Xác nhận phương thức thanh toán", "Bấm Xác nhận để ghi nhận sự kiện thu mua"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 16. Phiếu nhập kho
-- ============================================================
('00000000-0000-0000-0000-000000000016', 'warehouse-receipt', 'GENERAL',
 'Hướng dẫn ghi phiếu nhập kho',
 '["Chọn lô hàng đã vận chuyển đến", "Quét mã vận đơn hoặc mã truy xuất để đối soát", "Nhập thông tin nhập kho: số lượng, vị trí, người nhận", "Bấm Lưu để hoàn tất phiếu nhập kho"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 17. Điều kiện bảo quản
-- ============================================================
('00000000-0000-0000-0000-000000000017', 'storage-condition', 'GENERAL',
 'Hướng dẫn ghi điều kiện bảo quản',
 '["Chọn kho/lô hàng đang lưu trữ", "Nhập nhiệt độ, độ ẩm và thời gian đo", "Ghi chú tình trạng hàng hóa", "Bấm Lưu để cập nhật điều kiện bảo quản"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 18. Xác minh chuỗi sự kiện
-- ============================================================
('00000000-0000-0000-0000-000000000018', 'event-chain-verification', 'GENERAL',
 'Hướng dẫn xác minh chuỗi sự kiện',
 '["Chọn lô hoặc mã truy xuất cần kiểm tra", "Xem sơ đồ chuỗi sự kiện theo thời gian", "Kiểm tra dấu vân tay (hash) từng sự kiện", "Phát hiện điểm bất thường và gửi cảnh báo nếu cần"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),
('00000000-0000-0000-0000-000000000104', 'event-chain-verification', 'VT-04',
 'Hướng dẫn xác minh chuỗi sự kiện cho doanh nghiệp thu mua',
 '["Tìm lô hàng của doanh nghiệp mình", "Xem toàn bộ chuỗi sự kiện từ sản xuất đến vận chuyển", "Kiểm tra dấu vân tay và tính toàn vẹn dữ liệu", "Xuất báo cáo xác minh để lưu hồ sơ"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 19. Danh sách yêu cầu thu hồi
-- ============================================================
('00000000-0000-0000-0000-000000000019', 'recall-request-list', 'GENERAL',
 'Hướng dẫn danh sách yêu cầu thu hồi',
 '["Lọc danh sách yêu cầu theo trạng thái (PENDING/APPROVED/REJECTED)", "Mở yêu cầu để xem lý do, bằng chứng và lô liên quan", "Duyệt để kích hoạt thu hồi hoặc từ chối kèm lý do", "Theo dõi các lô đã thu hồi từ màn hình này"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 20. Tạo yêu cầu thu hồi
-- ============================================================
('00000000-0000-0000-0000-000000000020', 'recall-request-create', 'GENERAL',
 'Hướng dẫn tạo yêu cầu thu hồi',
 '["Chọn lô sản xuất cần thu hồi", "Nhập lý do thu hồi rõ ràng", "Đính kèm bằng chứng (ảnh/tài liệu)", "Bấm Gửi — yêu cầu chuyển đến quản lý duyệt"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 21. Chi tiết yêu cầu thu hồi
-- ============================================================
('00000000-0000-0000-0000-000000000021', 'recall-request-detail', 'GENERAL',
 'Hướng dẫn xử lý yêu cầu thu hồi',
 '["Xem thông tin lô, lý do và bằng chứng yêu cầu", "Xem ảnh hưởng: các lô hàng và mã truy xuất liên quan", "Nhấn Duyệt để thu hồi toàn bộ chuỗi hoặc Từ chối kèm lý do", "Theo dõi thông báo gửi đến doanh nghiệp thu mua"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 22. Thống kê lượt tra cứu
-- ============================================================
('00000000-0000-0000-0000-000000000022', 'report-lookup-statistics', 'GENERAL',
 'Hướng dẫn xem thống kê lượt tra cứu',
 '["Chọn khoảng thời gian cần thống kê", "Xem số lượt tra cứu, kênh tra cứu, mã được tra cứu nhiều", "Lọc theo sản phẩm hoặc khu vực nếu cần", "Xuất báo cáo ra file để chia sẻ"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 23. Phân tích diện tích canh tác
-- ============================================================
('00000000-0000-0000-0000-000000000023', 'report-crop-area-analysis', 'GENERAL',
 'Hướng dẫn phân tích diện tích canh tác',
 '["Chọn vụ mùa và khu vực cần phân tích", "Xem biểu đồ diện tích gieo trồng theo khu vực", "So sánh với các vụ trước", "Xuất số liệu phân tích nếu cần"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 24. So sánh năng suất theo vụ
-- ============================================================
('00000000-0000-0000-0000-000000000024', 'report-season-yield', 'GENERAL',
 'Hướng dẫn so sánh năng suất theo vụ',
 '["Chọn các vụ mùa cần so sánh", "Xem biểu đồ năng suất từng vụ", "Xem chi tiết theo khu vực/sản phẩm", "Xuất báo cáo so sánh"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 25. Báo cáo ngành
-- ============================================================
('00000000-0000-0000-0000-000000000025', 'report-industry', 'GENERAL',
 'Hướng dẫn xem báo cáo ngành',
 '["Chọn kỳ báo cáo (tháng/quý/năm)", "Xem thống kê toàn ngành về sản lượng, diện tích", "Lọc theo sản phẩm/tỉnh thành", "Xuất báo cáo ngành ra file"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 26. Nhật ký hoạt động
-- ============================================================
('00000000-0000-0000-0000-000000000026', 'report-activity-log', 'GENERAL',
 'Hướng dẫn xem nhật ký hoạt động',
 '["Chọn khoảng thời gian cần xem", "Lọc theo người dùng hoặc loại thao tác", "Xem chi tiết thay đổi từng bản ghi", "Xuất nhật ký hoạt động để lưu trữ"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 27. Nhật ký sự kiện thất bại
-- ============================================================
('00000000-0000-0000-0000-000000000027', 'report-failed-events', 'GENERAL',
 'Hướng dẫn xử lý sự kiện thất bại',
 '["Xem danh sách sự kiện thất bại (lỗi hash, thiếu dữ liệu)", "Mở chi tiết để xem nguyên nhân lỗi", "Sửa dữ liệu hoặc nhấn Thử lại", "Theo dõi trạng thái đồng bộ lại"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 28. Cảnh báo bất thường khi quét
-- ============================================================
('00000000-0000-0000-0000-000000000028', 'alert-scan-anomaly', 'GENERAL',
 'Hướng dẫn xử lý cảnh báo bất thường khi quét',
 '["Xem danh sách cảnh báo bất thường khi quét mã", "Lọc theo mức độ và thời gian", "Mở chi tiết để điều tra nguyên nhân", "Xử lý cảnh báo: đánh dấu hoặc cập nhật trạng thái"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 29. Thông báo
-- ============================================================
('00000000-0000-0000-0000-000000000029', 'notifications', 'GENERAL',
 'Hướng dẫn sử dụng thông báo',
 '["Xem danh sách thông báo mới", "Bấm vào thông báo để mở trang liên quan", "Đánh dấu đã đọc từng thông báo", "Lọc theo trạng thái đã đọc/chưa đọc"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 30. Danh sách dải mã
-- ============================================================
('00000000-0000-0000-0000-000000000030', 'admin-code-range-list', 'GENERAL',
 'Hướng dẫn quản lý dải mã',
 '["Xem danh sách dải mã đã cấp cho tổ chức", "Lọc theo trạng thái dải mã", "Tạo dải mã mới hoặc xem chi tiết dải mã", "Theo dõi số mã đã sử dụng/còn lại"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 31. Tạo dải mã
-- ============================================================
('00000000-0000-0000-0000-000000000031', 'admin-code-range-create', 'GENERAL',
 'Hướng dẫn tạo dải mã',
 '["Chọn tổ chức nhận dải mã", "Nhập số lượng mã và tiền tố (prefix)", "Chọn ngày hết hạn nếu có", "Bấm Tạo — dải mã được sinh tự động"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 32. Danh mục sản phẩm
-- ============================================================
('00000000-0000-0000-0000-000000000032', 'admin-product-categories', 'GENERAL',
 'Hướng dẫn quản lý danh mục sản phẩm',
 '["Xem danh sách danh mục sản phẩm", "Thêm/sửa tên danh mục và mô tả", "Đặt ngưỡng cảnh báo (tồn kho/khu vực) nếu có", "Lưu thay đổi để áp dụng ngay"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 33. Tiêu chuẩn
-- ============================================================
('00000000-0000-0000-0000-000000000033', 'admin-standards', 'GENERAL',
 'Hướng dẫn quản lý tiêu chuẩn',
 '["Xem danh sách tiêu chuẩn áp dụng", "Thêm tiêu chuẩn mới với tên, phiên bản, mô tả", "Gán tiêu chuẩn cho danh mục sản phẩm", "Xuất bản để áp dụng cho hệ thống"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 34. Mã truy xuất nghi ngờ
-- ============================================================
('00000000-0000-0000-0000-000000000034', 'admin-suspect-trace-codes', 'GENERAL',
 'Hướng dẫn xử lý mã truy xuất nghi ngờ',
 '["Xem danh sách mã truy xuất nghi ngờ", "Mở chi tiết mã để xem lịch sử sự kiện", "Phân tích dấu hiệu bất thường", "Cập nhật trạng thái xử lý cho mã"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 35. Sao lưu & khôi phục
-- ============================================================
('00000000-0000-0000-0000-000000000035', 'admin-backup-restore', 'GENERAL',
 'Hướng dẫn sao lưu & khôi phục dữ liệu',
 '["Xem lịch sao lưu tự động hàng ngày", "Tạo bản sao lưu thủ công trước thay đổi lớn", "Khôi phục từ bản sao lưu khi cần", "Kiểm tra trạng thái các bản sao lưu"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 36. Danh sách chứng nhận
-- ============================================================
('00000000-0000-0000-0000-000000000036', 'certification-list', 'GENERAL',
 'Hướng dẫn quản lý chứng nhận',
 '["Xem danh sách chứng nhận của tổ chức", "Lọc theo trạng thái/sản phẩm", "Tạo chứng nhận mới", "Mở chi tiết để xem hồ sơ đính kèm"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 37. Tạo chứng nhận
-- ============================================================
('00000000-0000-0000-0000-000000000037', 'certification-create', 'GENERAL',
 'Hướng dẫn tạo chứng nhận',
 '["Chọn sản phẩm/lô cần chứng nhận", "Nhập loại chứng nhận, đơn vị cấp, ngày hiệu lực", "Đính kèm hồ sơ/minh chứng", "Bấm Lưu — chứng nhận được gửi duyệt"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 38. Khóa API đối tác
-- ============================================================
('00000000-0000-0000-0000-000000000038', 'admin-api-keys', 'GENERAL',
 'Hướng dẫn quản lý khóa API đối tác',
 '["Xem danh sách khóa API đối tác", "Tạo khóa mới cho đối tác tích hợp", "Đặt giới hạn tần suất và ngày hết hạn", "Thu hồi khóa nếu đối tác không còn hợp tác"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),
('00000000-0000-0000-0000-000000000105', 'admin-api-keys', 'VT-02',
 'Hướng dẫn quản lý khóa API cho hợp tác xã',
 '["Xem danh sách khóa API của hợp tác xã", "Tạo khóa mới cho đối tác thu mua", "Đặt giới hạn tần suất gọi API", "Thu hồi khóa khi ngừng hợp tác"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 39. Xuất dữ liệu mở
-- ============================================================
('00000000-0000-0000-0000-000000000039', 'export-open-data', 'GENERAL',
 'Hướng dẫn xuất dữ liệu mở',
 '["Chọn bộ dữ liệu mở cần xuất (lô, sự kiện, truy xuất)", "Chọn định dạng và khoảng thời gian", "Xem trước dữ liệu trước khi xuất", "Tải file dữ liệu mở đã xuất"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 40. Quản lý thành viên & quyền
-- ============================================================
('00000000-0000-0000-0000-000000000040', 'member-permissions', 'GENERAL',
 'Hướng dẫn quản lý thành viên & quyền',
 '["Xem danh sách thành viên của tổ chức", "Thêm thành viên mới bằng email/số điện thoại", "Gán vai trò và quyền cho thành viên", "Khóa/vô hiệu tài khoản khi cần"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 41. Cấu hình phân quyền vai trò
-- ============================================================
('00000000-0000-0000-0000-000000000041', 'permission-config', 'GENERAL',
 'Hướng dẫn cấu hình phân quyền vai trò',
 '["Xem danh sách vai trò trong hệ thống", "Chọn vai trò để xem các quyền được cấp", "Bật/tắt quyền theo nhu cầu", "Lưu thay đổi — áp dụng ngay cho thành viên"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 42. Phản hồi sản phẩm
-- ============================================================
('00000000-0000-0000-0000-000000000042', 'product-feedback', 'GENERAL',
 'Hướng dẫn quản lý phản hồi sản phẩm',
 '["Xem danh sách phản hồi sản phẩm từ khách hàng", "Lọc theo sản phẩm/đánh giá", "Mở chi tiết phản hồi kèm mã truy xuất", "Xử lý hoặc gắn cờ phản hồi cần lưu ý"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 43. Sự kiện offline
-- ============================================================
('00000000-0000-0000-0000-000000000043', 'offline-events', 'GENERAL',
 'Hướng dẫn đồng bộ sự kiện offline',
 '["Xem danh sách sự kiện ghi offline chưa đồng bộ", "Chọn sự kiện cần đồng bộ", "Nhấn Đồng bộ — hệ thống gửi lên máy chủ", "Kiểm tra trạng thái thành công/thất bại"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 44. Danh sách khu vực canh tác
-- ============================================================
('00000000-0000-0000-0000-000000000044', 'farm-area-list', 'GENERAL',
 'Hướng dẫn quản lý khu vực canh tác',
 '["Xem danh sách khu vực canh tác", "Lọc theo trạng thái hoạt động", "Thêm khu vực mới hoặc sửa khu vực cũ", "Theo dõi diện tích và lô đang hoạt động"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 45. Chỉnh sửa khu vực canh tác
-- ============================================================
('00000000-0000-0000-0000-000000000045', 'farm-area-edit', 'GENERAL',
 'Hướng dẫn chỉnh sửa khu vực canh tác',
 '["Cập nhật tên và mã khu vực canh tác", "Cập nhật diện tích, vị trí (tọa độ nếu có)", "Chọn trạng thái hoạt động", "Bấm Lưu để cập nhật khu vực vào hệ thống"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 46. Quản lý danh mục vật tư đầu vào
-- ============================================================
('00000000-0000-0000-0000-000000000046', 'admin-input-materials', 'GENERAL',
 'Hướng dẫn quản lý danh mục vật tư đầu vào',
 '["Khai báo và quản lý danh mục vật tư dùng chung toàn hệ thống (Phân bón, Thuốc BVTV, Chế phẩm...)", "Cấu hình Thời gian cách ly (PHI) theo ngày cho từng loại vật tư", "Bật/Tắt công tắc trạng thái để Kích hoạt hoặc Ngừng sử dụng vật tư", "Bấm Thêm vật tư mới để khai báo vật tư mới kèm hoạt chất và hình ảnh"]',
 NULL, 0, '2026-08-17 00:00:00', '2026-08-17 00:00:00'),

-- ============================================================
-- 47. Hủy tem in hỏng
-- ============================================================
('00000000-0000-0000-0000-000000000047', 'cancel-labels', 'GENERAL',
 'Hướng dẫn đánh dấu hủy tem in hỏng & hoàn hạn mức',
 '["Gom danh sách tem in mờ, nhòe QR, lệch viền hoặc bị rách trong khi dán. Chỉ tem ở trạng thái Chưa kích hoạt (INACTIVE) mới được hủy.", "Chọn phương thức nhập mã: Chọn Theo khoảng mã (Range) nếu dải tem liên tiếp, hoặc Chọn Nhập từng mã lẻ (Single) và dùng súng quét mã vạch USB / dán danh sách mã.", "Chọn lý do tiêu hủy: Chọn lý do phù hợp (In mờ, in lệch, bong tróc). Nếu chọn Lý do khác, điền ghi chú giải trình chi tiết tối thiểu 10 ký tự.", "Xác nhận & Hoàn hạn mức: Bấm Xác nhận hủy & Hoàn hạn mức. Mã tem đổi sang CANCELLED và hạn mức dải mã của HTX được tự động hoàn trả ngay lập tức."]',
 'Ví dụ: Hủy khoảng mã HTX01-00000010 đến HTX01-00000025 do kẹt giấy máy in lô #2.', 0, '2026-08-27 00:00:00', '2026-08-27 00:00:00'),

-- ============================================================
-- 48. Lịch sử hủy tem
-- ============================================================
('00000000-0000-0000-0000-000000000048', 'label-cancellation-history', 'GENERAL',
 'Hướng dẫn tra cứu lịch sử hủy tem in hỏng',
 '["Xem thẻ tổng quan thống kê: Tổng số tem đã hủy, số đợt thao tác và hạn mức dải mã đã hoàn trả.", "Sử dụng công cụ tìm kiếm theo mã tem, ghi chú giải trình hoặc tài khoản người thực hiện.", "Lọc danh sách theo phương thức (Khoảng mã / Mã lẻ) hoặc theo lý do tiêu hủy.", "Sử dụng thanh phân trang ở chân bảng để chuyển giữa các trang (tối đa 10 đợt/trang)."]',
 'Ví dụ: Lọc lý do "In hỏng/mờ/nhòe QR" để thống kê tổng số tem bị hỏng do mực in.', 0, '2026-08-27 00:00:00', '2026-08-27 00:00:00');