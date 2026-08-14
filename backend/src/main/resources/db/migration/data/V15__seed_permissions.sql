-- ============================================================
-- V15: Seed all required permissions (master data)
-- Uses INSERT IGNORE with unique key on (resource, action)
-- ============================================================

INSERT IGNORE INTO permissions(resource, action, description) VALUES

-- Organization
('organization','READ','Xem thông tin tổ chức'),
('organization','UPDATE','Cập nhật thông tin tổ chức'),

-- Farm Area
('farm_area','CREATE','Tạo vùng trồng'),
('farm_area','READ','Xem vùng trồng'),
('farm_area','UPDATE','Cập nhật vùng trồng'),
('farm_area','DELETE','Xóa vùng trồng'),

-- Production Lot
('production_lot','CREATE','Tạo lô sản xuất'),
('production_lot','READ','Xem lô sản xuất'),
('production_lot','UPDATE','Cập nhật lô sản xuất'),
('production_lot','APPROVE','Duyệt lô sản xuất'),

-- Farm Log
('farm_log','CREATE','Tạo nhật ký'),
('farm_log','READ','Xem nhật ký'),
('farm_log','UPDATE','Cập nhật nhật ký'),
('farm_log','VERIFY','Xác minh nhật ký'),

-- Shipment
('shipment','CREATE','Tạo lô hàng'),
('shipment','READ','Xem lô hàng'),
('shipment','UPDATE','Cập nhật lô hàng'),
('shipment','EXPORT','Xuất hồ sơ truy xuất'),

-- Trace Code
('trace_code','CREATE','Sinh mã truy xuất'),
('trace_code','READ','Xem mã truy xuất'),
('trace_code','ACTIVATE','Kích hoạt mã'),

-- Chain Event
('chain_event','CREATE','Ghi sự kiện chuỗi'),
('chain_event','READ','Xem dòng sự kiện'),
('chain_event','UPDATE','Đính chính sự kiện'),

-- Certification
('certification','CREATE','Tạo chứng nhận'),
('certification','READ','Xem chứng nhận'),
('certification','UPDATE','Cập nhật chứng nhận'),

-- Standard
('standard','CREATE','Tạo tiêu chuẩn'),
('standard','READ','Xem tiêu chuẩn'),
('standard','UPDATE','Cập nhật tiêu chuẩn'),

-- Product Category
('product_category','CREATE','Tạo loại nông sản'),
('product_category','READ','Xem loại nông sản'),
('product_category','UPDATE','Cập nhật loại nông sản'),

-- Organization User
('organization_user','CREATE','Thêm thành viên'),
('organization_user','READ','Xem thành viên'),
('organization_user','UPDATE','Cập nhật thành viên'),
('organization_user','DELETE','Xóa thành viên'),

-- Role Permission
('role_permission','READ','Xem phân quyền'),
('role_permission','UPDATE','Cấu hình phân quyền'),

-- Notification
('notification','READ','Xem thông báo'),

-- Alert
('alert','READ','Xem cảnh báo'),
('alert','UPDATE','Xử lý cảnh báo'),

-- Report
('report','READ','Xem báo cáo'),
('report','EXPORT','Xuất báo cáo'),

-- Scan Statistics
('scan_statistics','READ','Xem thống kê lượt quét'),

-- Activity Log
('activity_log','READ','Xem lịch sử hoạt động'),

-- Product Feedback
('product_feedback','READ','Xem phản ánh sản phẩm'),

-- Code Range
('code_range','CREATE','Tạo dải mã truy xuất'),
('code_range','READ','Xem dải mã truy xuất'),
('code_range','UPDATE','Cập nhật dải mã truy xuất'),

-- Traceability (public-facing trace page)
('traceability','READ','Xem trang truy xuất nguồn gốc'),

-- Recall
('recall','CREATE','Tạo yêu cầu thu hồi'),
('recall','READ','Xem thu hồi lô'),

-- Export
('export','CREATE','Xuất dữ liệu'),
('export','READ','Xem lịch sử xuất dữ liệu'),

-- User (system-level user management)
('user','CREATE','Tạo người dùng'),
('user','READ','Xem người dùng'),
('user','UPDATE','Cập nhật người dùng'),
('user','DELETE','Xóa người dùng');