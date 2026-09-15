-- ============================================================
-- NCL-07-CN-008 — Seed kiểm thử thủ công "Mức độ sử dụng nền tảng"
--
-- Mục đích:
--   Tạo bộ dữ liệu demo cho Dashboard mức độ sử dụng nền tảng:
--     - HTXA (HTX Chè Tân Cương): tổ chức HOẠT ĐỘNG mạnh, có số liệu
--       tăng trưởng ở kỳ hiện tại so kỳ trước (cả 6 metric)
--     - HTXB (HTX Rau Sạch): tổ chức NGỪNG HOẠT ĐỘNG từ 2026-06-01
--       (>= 30 ngày) -> "Cần liên hệ hỗ trợ", không có số liệu 2 kỳ
--     - HTXC (HTX Mới Thành Lập): tổ chức MỚI tạo 2026-09-14
--       -> "Chưa có dữ liệu"
--
-- Kỳ mặc định của dashboard: current = 2026-08-16..2026-09-14 (30 ngày),
-- previous = 2026-07-17..2026-08-15. Nếu test vào ngày khác, truyền
-- startDate=2026-08-16&endDate=2026-09-14 để tái lập đúng số liệu này.
--
-- Cách chạy (PowerShell):
--   & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u nguongocso -pnguongocso nguon_goc_so < docs/testing/NCL-07-CN-008-seed.sql
--   (hoặc: $env:MYSQL_PWD="nguongocso" rồi dùng mysql -u nguongocso nguon_goc_so)
--
-- Idempotent: DELETE các ID cố định trước, INSERT IGNORE sau -> chạy lại an toàn.
-- ============================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 0. Các định danh dùng chung
-- ============================================================
SET @org_htxa = 'aaa00001-0000-0000-0000-000000000001'; -- HTX Chè Tân Cương
SET @org_htxb = 'bbb00002-0000-0000-0000-000000000002'; -- HTX Rau Sạch
SET @org_htxc = 'ccc00003-0000-0000-0000-000000000003'; -- HTX Mới Thành Lập

SET @user_mgrA = 'ddd00004-0000-0000-0000-000000000004'; -- managerA (VT-02, HTXA)
SET @cat_che   = 'cat00001-0000-0000-0000-000000000001'; -- Chè

-- Tài khoản demo HTXA (hash lấy từ admin/admin123 -> đăng nhập admin123)
DELETE FROM users WHERE user_id = @user_mgrA;
INSERT INTO users (user_id, user_name, password_hash, full_name, status, created_at, updated_at)
SELECT @user_mgrA, 'managerA', password_hash, 'Quản lý HTX A', 'ACTIVE', NOW(), NOW()
FROM users WHERE user_name = 'admin' LIMIT 1;

DELETE FROM organization_users WHERE id = 'eee00005-0000-0000-0000-000000000005';
INSERT IGNORE INTO organization_users (id, organization_id, user_id, role_id, joined_at, status)
VALUES ('eee00005-0000-0000-0000-000000000005', @org_htxa, @user_mgrA, 2, '2026-01-11 08:00:00', 'ACTIVE');

-- Tổ chức demo
INSERT IGNORE INTO organizations (organization_id, name, code, type, status, created_at, updated_at) VALUES
(@org_htxa, 'HTX Chè Tân Cương', 'HTXA', 'COOPERATIVE', 'ACTIVE', '2026-01-10 08:00:00', '2026-01-10 08:00:00'),
(@org_htxb, 'HTX Rau Sạch',     'HTXB', 'COOPERATIVE', 'ACTIVE', '2026-02-15 08:00:00', '2026-02-15 08:00:00'),
(@org_htxc, 'HTX Mới Thành Lập','HTXC', 'COOPERATIVE', 'ACTIVE', '2026-09-14 08:00:00', '2026-09-14 08:00:00');

INSERT IGNORE INTO product_categories (id, name, is_active, requires_inspection)
VALUES (@cat_che, 'Chè', 1, 0);

-- ============================================================
-- 1. Dọn dữ liệu cũ (theo ID cố định) để chạy lại idempotent
-- ============================================================
DELETE FROM trace_code_scan_logs WHERE id IN (
  'scn10001-0000-0000-0000-000000000001',
  'scn10002-0000-0000-0000-000000000002',
  'scn10003-0000-0000-0000-000000000003',
  'scn10004-0000-0000-0000-000000000004');
DELETE FROM trace_codes WHERE id IN (
  'trc10001-0000-0000-0000-000000000001',
  'trc10002-0000-0000-0000-000000000002',
  'trc10003-0000-0000-0000-000000000003');
DELETE FROM chain_events WHERE id IN (
  'evt10001-0000-0000-0000-000000000001',
  'evt10002-0000-0000-0000-000000000002',
  'evt10003-0000-0000-0000-000000000003',
  'evt10004-0000-0000-0000-000000000004');
DELETE FROM shipments WHERE id IN (
  'shp10001-0000-0000-0000-000000000001',
  'shp10002-0000-0000-0000-000000000002',
  'shp10003-0000-0000-0000-000000000003',
  'shp10004-0000-0000-0000-000000000004',
  'shp10005-0000-0000-0000-000000000005');
DELETE FROM farm_logs WHERE id IN (
  'log10001-0000-0000-0000-000000000001',
  'log10002-0000-0000-0000-000000000002',
  'log10003-0000-0000-0000-000000000003',
  'log10004-0000-0000-0000-000000000004',
  'log10005-0000-0000-0000-000000000005',
  'log10006-0000-0000-0000-000000000006',
  'log10007-0000-0000-0000-000000000007');
DELETE FROM production_lot WHERE id IN (
  'lot10001-0000-0000-0000-000000000001',
  'lot10002-0000-0000-0000-000000000002',
  'lot10003-0000-0000-0000-000000000003',
  'lot10004-0000-0000-0000-000000000004',
  'lot10005-0000-0000-0000-000000000005',
  'lot20001-0000-0000-0000-000000000006');
DELETE FROM activity_logs WHERE id IN (
  'act10001-0000-0000-0000-000000000001',
  'act10002-0000-0000-0000-000000000002',
  'act10003-0000-0000-0000-000000000003',
  'act10004-0000-0000-0000-000000000004',
  'act10005-0000-0000-0000-000000000005',
  'act20001-0000-0000-0000-000000000005',
  'act20001-0000-0000-0000-000000000006');

-- ============================================================
-- 2. Lô sản xuất
--    HTXA: 3 lô kỳ hiện tại (2026-08-16..2026-09-14) + 2 lô kỳ trước
--    HTXB: 1 lô cũ (2026-06-01)
-- ============================================================
INSERT INTO production_lot (id, organization_id, product_category_id, name, expected_quantity, status, created_at, updated_at) VALUES
('lot10001-0000-0000-0000-000000000001', @org_htxa, @cat_che, 'Lô chè A1',  100, 'APPROVED', '2026-08-20 08:00:00', '2026-08-20 08:00:00'),
('lot10002-0000-0000-0000-000000000002', @org_htxa, @cat_che, 'Lô chè A2',  200, 'APPROVED', '2026-09-01 08:00:00', '2026-09-01 08:00:00'),
('lot10003-0000-0000-0000-000000000003', @org_htxa, @cat_che, 'Lô chè A3',  150, 'APPROVED', '2026-09-10 08:00:00', '2026-09-10 08:00:00'),
('lot10004-0000-0000-0000-000000000004', @org_htxa, @cat_che, 'Lô chè A0-1',120, 'APPROVED', '2026-07-20 08:00:00', '2026-07-20 08:00:00'),
('lot10005-0000-0000-0000-000000000005', @org_htxa, @cat_che, 'Lô chè A0-2',130, 'APPROVED', '2026-08-10 08:00:00', '2026-08-10 08:00:00'),
('lot20001-0000-0000-0000-000000000006', @org_htxb, @cat_che, 'Lô rau B1',   80, 'APPROVED', '2026-06-01 08:00:00', '2026-06-01 08:00:00');

-- ============================================================
-- 3. Lô hàng (shipment) — để gắn sự kiện chuỗi + tem
-- ============================================================
INSERT INTO shipments (id, production_lot_id, organization_id, name, total_quantity, status, created_by, created_at, updated_at) VALUES
('shp10001-0000-0000-0000-000000000001', 'lot10001-0000-0000-0000-000000000001', @org_htxa, 'SHP-A1',  100, 'ACTIVATED', @user_mgrA, '2026-08-20 09:00:00', '2026-08-20 09:00:00'),
('shp10002-0000-0000-0000-000000000002', 'lot10002-0000-0000-0000-000000000002', @org_htxa, 'SHP-A2',  200, 'ACTIVATED', @user_mgrA, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
('shp10003-0000-0000-0000-000000000003', 'lot10003-0000-0000-0000-000000000003', @org_htxa, 'SHP-A3',  150, 'ACTIVATED', @user_mgrA, '2026-09-10 09:00:00', '2026-09-10 09:00:00'),
('shp10004-0000-0000-0000-000000000004', 'lot10004-0000-0000-0000-000000000004', @org_htxa, 'SHP-A0-1',120, 'ACTIVATED', @user_mgrA, '2026-07-20 09:00:00', '2026-07-20 09:00:00'),
('shp10005-0000-0000-0000-000000000005', 'lot10005-0000-0000-0000-000000000005', @org_htxa, 'SHP-A0-2',130, 'ACTIVATED', @user_mgrA, '2026-08-10 09:00:00', '2026-08-10 09:00:00');

-- ============================================================
-- 4. Sự kiện chuỗi (chain_events) — is_correction = 0
--    HTXA: 3 sự kiện kỳ hiện tại + 1 sự kiện kỳ trước
-- ============================================================
INSERT INTO chain_events (id, shipment_id, event_type, recorded_at, recorded_by, recorded_organization_id, created_at, is_correction) VALUES
('evt10001-0000-0000-0000-000000000001', 'shp10001-0000-0000-0000-000000000001', 'HARVEST',    '2026-08-22 08:30:00', @user_mgrA, @org_htxa, '2026-08-22 08:30:00', 0),
('evt10002-0000-0000-0000-000000000002', 'shp10002-0000-0000-0000-000000000002', 'PACKAGING',  '2026-09-03 08:30:00', @user_mgrA, @org_htxa, '2026-09-03 08:30:00', 0),
('evt10003-0000-0000-0000-000000000003', 'shp10003-0000-0000-0000-000000000003', 'TRANSPORT',  '2026-09-11 08:30:00', @user_mgrA, @org_htxa, '2026-09-11 08:30:00', 0),
('evt10004-0000-0000-0000-000000000004', 'shp10005-0000-0000-0000-000000000005', 'HARVEST',    '2026-08-11 08:30:00', @user_mgrA, @org_htxa, '2026-08-11 08:30:00', 0);

-- ============================================================
-- 5. Tem đã kích hoạt (trace_codes) — status ACTIVATED, activated_at có giá trị
--    HTXA: 2 tem kỳ hiện tại + 1 tem kỳ trước
-- ============================================================
INSERT INTO trace_codes (id, shipment_id, code_value, status, activated_at, activated_by, created_at) VALUES
('trc10001-0000-0000-0000-000000000001', 'shp10001-0000-0000-0000-000000000001', 'TRC-A1',   'ACTIVE', '2026-08-23 10:00:00', @user_mgrA, '2026-08-23 10:00:00'),
('trc10002-0000-0000-0000-000000000002', 'shp10002-0000-0000-0000-000000000002', 'TRC-A2',   'ACTIVE', '2026-09-05 10:00:00', @user_mgrA, '2026-09-05 10:00:00'),
('trc10003-0000-0000-0000-000000000003', 'shp10005-0000-0000-0000-000000000005', 'TRC-A0-2', 'ACTIVE', '2026-08-12 10:00:00', @user_mgrA, '2026-08-12 10:00:00');

-- ============================================================
-- 6. Tra cứu công khai (trace_code_scan_logs)
--    HTXA: 3 lượt kỳ hiện tại + 1 lượt kỳ trước
-- ============================================================
INSERT INTO trace_code_scan_logs (id, trace_code_id, scanned_at, is_abnormal) VALUES
('scn10001-0000-0000-0000-000000000001', 'trc10001-0000-0000-0000-000000000001', '2026-08-24 11:00:00', 0),
('scn10002-0000-0000-0000-000000000002', 'trc10002-0000-0000-0000-000000000002', '2026-09-06 11:00:00', 0),
('scn10003-0000-0000-0000-000000000003', 'trc10002-0000-0000-0000-000000000002', '2026-09-12 11:00:00', 0),
('scn10004-0000-0000-0000-000000000004', 'trc10003-0000-0000-0000-000000000003', '2026-08-13 11:00:00', 0);

-- ============================================================
-- 7. Nhật ký sản xuất (farm_logs)
--    HTXA: 4 dòng kỳ hiện tại + 2 dòng kỳ trước (+ đủ nhóm enum cơ bản)
-- ============================================================
INSERT INTO farm_logs (id, production_lot_id, activity_type, executed_date, created_by, created_at) VALUES
('log10001-0000-0000-0000-000000000001', 'lot10001-0000-0000-0000-000000000001', 'PLANTING',   '2026-08-21', @user_mgrA, '2026-08-21 08:00:00'),
('log10002-0000-0000-0000-000000000002', 'lot10002-0000-0000-0000-000000000002', 'WATERING',   '2026-09-01', @user_mgrA, '2026-09-01 08:00:00'),
('log10003-0000-0000-0000-000000000003', 'lot10002-0000-0000-0000-000000000002', 'FERTILIZING','2026-09-02', @user_mgrA, '2026-09-02 08:00:00'),
('log10004-0000-0000-0000-000000000004', 'lot10003-0000-0000-0000-000000000003', 'WEEDING',    '2026-09-12', @user_mgrA, '2026-09-12 08:00:00'),
('log10005-0000-0000-0000-000000000005', 'lot10004-0000-0000-0000-000000000004', 'PLANTING',   '2026-07-21', @user_mgrA, '2026-07-21 08:00:00'),
('log10006-0000-0000-0000-000000000006', 'lot10005-0000-0000-0000-000000000005', 'WATERING',   '2026-08-11', @user_mgrA, '2026-08-11 08:00:00');

-- ============================================================
-- 8. Nhật ký hoạt động hệ thống (activity_logs) — nguồn activeUsers + lastActivity
--    HTXA: 3 dòng kỳ hiện tại + 2 dòng kỳ trước (user managerA = 1 user active/2 kỳ)
--    HTXB: 1 dòng cũ (2026-06-01)
-- ============================================================
INSERT INTO activity_logs (id, organization_id, user_id, username, full_name, action, description, created_at) VALUES
('act10001-0000-0000-0000-000000000001', @org_htxa, @user_mgrA, 'managerA', 'Quản lý HTX A', 'CREATE_LOT',       'Tạo lô chè A1',   '2026-08-20 08:05:00'),
('act10002-0000-0000-0000-000000000002', @org_htxa, @user_mgrA, 'managerA', 'Quản lý HTX A', 'CREATE_LOT',       'Tạo lô chè A2',   '2026-09-01 08:05:00'),
('act10003-0000-0000-0000-000000000003', @org_htxa, @user_mgrA, 'managerA', 'Quản lý HTX A', 'CREATE_FARM_LOG', 'Ghi nhật ký',     '2026-09-12 08:05:00'),
('act10004-0000-0000-0000-000000000004', @org_htxa, @user_mgrA, 'managerA', 'Quản lý HTX A', 'CREATE_LOT',       'Tạo lô chè A0-1', '2026-07-20 08:05:00'),
('act10005-0000-0000-0000-000000000005', @org_htxa, @user_mgrA, 'managerA', 'Quản lý HTX A', 'CREATE_FARM_LOG', 'Ghi nhật ký',     '2026-08-11 08:05:00'),
('act20001-0000-0000-0000-000000000006', @org_htxb, @user_mgrA, 'managerA', 'Quản lý HTX A', 'CREATE_LOT',       'Tạo lô rau B1',   '2026-06-01 08:05:00');

SET FOREIGN_KEY_CHECKS = 1;