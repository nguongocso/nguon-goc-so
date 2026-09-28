-- ============================================================
-- Seed data for manual E2E testing NCL-04-CN-007.
-- Run against MySQL dev (port 3307, db: nguon_goc_so).
--
-- Mục đích: Tạo các lô sản xuất (production_lot) trạng thái APPROVED
-- thuộc tổ chức DEMO_HTX, có expected_quantity lớn (500–2000 KG)
-- để khi tạo lô hàng và nhập số lượng sẽ VƯỢT hạn mức dải mã còn lại.
--
-- Context: DEMO_HTX có total_limit=1000, used_count=950 → còn 50 mã.
-- Khi user tạo lô hàng từ các lô này với số lượng > 50, hệ thống sẽ:
--   1. Cảnh báo hạn mức sắp hết / không đủ mã
--   2. Yêu cầu cấp mã mới (luồng NCL-04-CN-007)
--
-- Các lô này CHƯA đóng gói (APPROVED), chưa sinh mã truy xuất,
-- đủ điều kiện để test luồng tạo lô hàng → cảnh báo → yêu cầu bổ sung mã.
--
-- Idempotent: INSERT IGNORE + UUID cố định dễ nhớ.
-- KHÔNG commit file này (docs/sample-data/ không track trên CI).
--
-- Cách chạy:
--   Get-Content docs\sample-data\seed_production_lots_for_shipment_test.sql |
--     docker exec -i nguon-goc-so-mysql-1 mysql -unguongocso -p29012005 nguon_goc_so
-- ============================================================

-- ============================================================
-- LÔ 1: Lô Xoài APPROVED — expected_quantity = 500 KG
-- Mục đích: Test tạo lô hàng với số lượng 500 > 50 mã còn lại.
--           Kích hoạt cảnh báo NEARLY_EXHAUSTED (<20% remaining).
-- ============================================================
INSERT IGNORE INTO production_lot
    (id, organization_id, farm_area_id, product_category_id, name,
     expected_quantity, expected_quantity_unit, actual_quantity,
     planting_date, harvest_date, status, approval_notes,
     created_by, approved_by, created_at, updated_at)
SELECT
    'aaaaaaaa-bbbb-cccc-dddd-eeeeeeee01',
    '327a3a0e-a396-11f1-aea2-32ec817c7ea4',
    '00000000-0000-0000-0000-000100000003',
    (SELECT id FROM product_categories WHERE name = 'Xoài' LIMIT 1),
    'Lô Xoài Test Shipment 01',
    500,
    'KG',
    NULL,
    DATE '2026-06-01',
    DATE '2026-09-01',
    'APPROVED',
    'Duyệt cho test NCL-04-CN-007 — vượt hạn mức dải mã',
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE organization_id = '327a3a0e-a396-11f1-aea2-32ec817c7ea4')
  AND EXISTS (SELECT 1 FROM product_categories WHERE name = 'Xoài');

-- ============================================================
-- LÔ 2: Lô Sầu riêng APPROVED — expected_quantity = 1000 KG
-- Mục đích: Test tạo lô hàng với số lượng 1000 >> 50 mã còn lại.
--           Số lượng bằng đúng total_limit hiện tại (1000),
--           chắc chắn vượt xa phần còn lại (50).
-- ============================================================
INSERT IGNORE INTO production_lot
    (id, organization_id, farm_area_id, product_category_id, name,
     expected_quantity, expected_quantity_unit, actual_quantity,
     planting_date, harvest_date, status, approval_notes,
     created_by, approved_by, created_at, updated_at)
SELECT
    'aaaaaaaa-bbbb-cccc-dddd-eeeeeeee02',
    '327a3a0e-a396-11f1-aea2-32ec817c7ea4',
    '00000000-0000-0000-0000-000100000005',
    (SELECT id FROM product_categories WHERE name = 'Sầu riêng' LIMIT 1),
    'Lô Sầu riêng Test Shipment 02',
    1000,
    'KG',
    NULL,
    DATE '2026-05-15',
    DATE '2026-08-20',
    'APPROVED',
    'Duyệt cho test NCL-04-CN-007 — vượt hạn mức dải mã',
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE organization_id = '327a3a0e-a396-11f1-aea2-32ec817c7ea4')
  AND EXISTS (SELECT 1 FROM product_categories WHERE name = 'Sầu riêng');

-- ============================================================
-- LÔ 3: Lô Hồ tiêu APPROVED — expected_quantity = 1500 KG
-- Mục đích: Test tạo lô hàng với số lượng 1500 >> 50 mã còn lại.
--           Vượt cả total_limit (1000) của dải mã, kiểm tra logic
--           yêu cầu cấp mã mới khi tổng nhu cầu > giới hạn tối đa.
-- ============================================================
INSERT IGNORE INTO production_lot
    (id, organization_id, farm_area_id, product_category_id, name,
     expected_quantity, expected_quantity_unit, actual_quantity,
     planting_date, harvest_date, status, approval_notes,
     created_by, approved_by, created_at, updated_at)
SELECT
    'aaaaaaaa-bbbb-cccc-dddd-eeeeeeee03',
    '327a3a0e-a396-11f1-aea2-32ec817c7ea4',
    '00000000-0000-0000-0000-000100000006',
    (SELECT id FROM product_categories WHERE name = 'Hồ tiêu' LIMIT 1),
    'Lô Hồ tiêu Test Shipment 03',
    1500,
    'KG',
    NULL,
    DATE '2026-04-01',
    DATE '2026-08-15',
    'APPROVED',
    'Duyệt cho test NCL-04-CN-007 — vượt hạn mức dải mã',
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE organization_id = '327a3a0e-a396-11f1-aea2-32ec817c7ea4')
  AND EXISTS (SELECT 1 FROM product_categories WHERE name = 'Hồ tiêu');

-- ============================================================
-- LÔ 4: Lô Cà phê APPROVED — expected_quantity = 2000 KG
-- Mục đích: Test boundary case — số lượng lớn nhất (2000), gấp đôi
--           total_limit hiện tại. Kiểm tra hệ thống xử lý đúng khi
--           nhu cầu mã vượt quá xa khả năng cấp phát hiện tại.
-- ============================================================
INSERT IGNORE INTO production_lot
    (id, organization_id, farm_area_id, product_category_id, name,
     expected_quantity, expected_quantity_unit, actual_quantity,
     planting_date, harvest_date, status, approval_notes,
     created_by, approved_by, created_at, updated_at)
SELECT
    'aaaaaaaa-bbbb-cccc-dddd-eeeeeeee04',
    '327a3a0e-a396-11f1-aea2-32ec817c7ea4',
    '00000000-0000-0000-0000-000100000008',
    (SELECT id FROM product_categories WHERE name = 'Cà phê' LIMIT 1),
    'Lô Cà phê Test Shipment 04',
    2000,
    'KG',
    NULL,
    DATE '2026-03-01',
    DATE '2026-07-30',
    'APPROVED',
    'Duyệt cho test NCL-04-CN-007 — vượt hạn mức dải mã',
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE organization_id = '327a3a0e-a396-11f1-aea2-32ec817c7ea4')
  AND EXISTS (SELECT 1 FROM product_categories WHERE name = 'Cà phê');

-- ============================================================
-- LÔ 5: Lô Nho APPROVED — expected_quantity = 800 KG
-- Mục đích: Test trường hợp trung bình (800 > 50 nhưng < 1000).
--           Dùng để test nhiều lô cùng lúc: tạo lô hàng từ lô 1 + lô 5
--           tổng = 1300, kiểm tra cảnh báo cộng dồn chính xác.
-- ============================================================
INSERT IGNORE INTO production_lot
    (id, organization_id, farm_area_id, product_category_id, name,
     expected_quantity, expected_quantity_unit, actual_quantity,
     planting_date, harvest_date, status, approval_notes,
     created_by, approved_by, created_at, updated_at)
SELECT
    'aaaaaaaa-bbbb-cccc-dddd-eeeeeeee05',
    '327a3a0e-a396-11f1-aea2-32ec817c7ea4',
    '00000000-0000-0000-0000-000100000001',
    (SELECT id FROM product_categories WHERE name = 'Nho' LIMIT 1),
    'Lô Nho Test Shipment 05',
    800,
    'KG',
    NULL,
    DATE '2026-05-01',
    DATE '2026-08-25',
    'APPROVED',
    'Duyệt cho test NCL-04-CN-007 — vượt hạn mức dải mã',
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE organization_id = '327a3a0e-a396-11f1-aea2-32ec817c7ea4')
  AND EXISTS (SELECT 1 FROM product_categories WHERE name = 'Nho');

-- ============================================================
-- Tóm tắt dữ liệu đã seed:
--
-- | # | ID                                     | Tên                          | SL dự kiến | Danh mục      | Trạng thái |
-- |---|----------------------------------------|------------------------------|------------|---------------|------------|
-- | 1 | aaaaaaaa-bbbb-cccc-dddd-eeeeeeee01     | Lô Xoài Test Shipment 01     | 500 KG     | Xoài          | APPROVED   |
-- | 2 | aaaaaaaa-bbbb-cccc-dddd-eeeeeeee02     | Lô Sầu riêng Test Shipment 02| 1000 KG    | Sầu riêng     | APPROVED   |
-- | 3 | aaaaaaaa-bbbb-cccc-dddd-eeeeeeee03     | Lô Hồ tiêu Test Shipment 03  | 1500 KG    | Hồ tiêu       | APPROVED   |
-- | 4 | aaaaaaaa-bbbb-cccc-dddd-eeeeeeee04     | Lô Cà phê Test Shipment 04   | 2000 KG    | Cà phê        | APPROVED   |
-- | 5 | aaaaaaaa-bbbb-cccc-dddd-eeeeeeee05     | Lô Nho Test Shipment 05      | 800 KG     | Nho           | APPROVED   |
--
-- Tất cả thuộc tổ chức DEMO_HTX (327a3a0e-a396-11f1-aea2-32ec817c7ea4).
-- Người tạo & duyệt: orgmanager (VT-02).
-- Dải mã DEMO-HTX-01: total_limit=1000, used_count=950 → còn 50 mã.
-- Mọi lô đều có expected_quantity > 50 → kích hoạt cảnh báo khi tạo lô hàng.
--
-- Lưu ý:
-- - File này KHÔNG được commit (docs/sample-data/ không track trên CI).
-- - Chạy thủ công trên MySQL dev trước khi test E2E.
-- - Sau khi test xong, có thể xóa các bản ghi này hoặc reset DB.
-- ============================================================