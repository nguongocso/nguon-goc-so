-- ============================================================
-- NCL-02-CN-007 — Dữ liệu kiểm thử thủ công "Tạo lô từ vụ trước"
--
-- Mục đích:
--   Seed dữ liệu test riêng biệt (prefix TEST_NCL02_CN007_) cho toàn
--   bộ kịch bản kiểm thử thủ công. KHÔNG đụng tới dữ liệu demo thật.
--
-- Cách chạy (PowerShell, chạy trên container MySQL dev):
--   docker compose exec -T mysql mysql --default-character-set=utf8mb4 \
--     -unguongocso -p29012005 nguon_goc_so < docs/testing/NCL-02-CN-007-seed.sql
--
-- Idempotent: mọi ID cố định + INSERT IGNORE -> chạy lại nhiều lần an toàn.
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 0. Các định danh dùng chung
-- ============================================================
SET @org_htx = '327a3a0e-a396-11f1-aea2-32ec817c7ea4';       -- DEMO_HTX
SET @org_nsv = '327a40dc-a396-11f1-aea2-32ec817c7ea4';       -- DEMO_NSV
SET @user_orgmanager = '327b0665-a396-11f1-aea2-32ec817c7ea4'; -- VT-02 (DEMO_HTX)
SET @user_procurement = '327b09c6-a396-11f1-aea2-32ec817c7ea4'; -- VT-04 (DEMO_NSV)

SET @cat_lua = '00000000-0000-0000-0000-000800000004';       -- Lúa
SET @cat_che = '00000000-0000-0000-0000-000800000002';       -- Chè
SET @cat_xoai = '00000000-0000-0000-0000-000800000003';      -- Xoài

SET @std_vietgap = 'f0892236-d181-4b3c-a4cd-2d394829a877';   -- VietGAP
SET @std_globalgap = '9e24816d-b3e5-4531-b990-2d2f0432f1f3'; -- GlobalG.A.P.

-- ============================================================
-- 1. Farm areas mới cho test
-- ============================================================
-- 1.1 Vùng trồng riêng cho lô có history (DEMO_HTX, active)
INSERT IGNORE INTO farm_areas
    (id, organization_id, crop_type, name, area, area_unit, location, is_active, created_at, updated_at)
VALUES
    ('c0000000-0000-0000-0000-000100000001', @org_htx, @cat_lua,
     'TEST_NCL02_CN007_VUNG_TRONG_HISTORY', 2.00, 'HA',
     ST_GeomFromText('POINT(105.8804 21.0604)'), 1, NOW(), NOW());

-- 1.2 Vùng trồng INACTIVE cho TC-07 (DEMO_HTX, is_active = 0)
INSERT IGNORE INTO farm_areas
    (id, organization_id, crop_type, name, area, area_unit, location, is_active, created_at, updated_at)
VALUES
    ('c0000000-0000-0000-0000-000100000002', @org_htx, @cat_lua,
     'TEST_NCL02_CN007_VUNG_TRONG_INACTIVE', 1.50, 'HA',
     ST_GeomFromText('POINT(106.0001 21.0001)'), 0, NOW(), NOW());

-- 1.3 Vùng trồng thuộc DEMO_NSV cho TC-08 (active)
INSERT IGNORE INTO farm_areas
    (id, organization_id, crop_type, name, area, area_unit, location, is_active, created_at, updated_at)
VALUES
    ('c0000000-0000-0000-0000-000100000003', @org_nsv, @cat_xoai,
     'TEST_NCL02_CN007_VUNG_TRONG_NSV', 3.00, 'HA',
     ST_GeomFromText('POINT(106.0002 21.0002)'), 1, NOW(), NOW());

-- ============================================================
-- 2. Production lots (lô mẫu test)
-- ============================================================
-- 2.1 HAPPY SOURCE: lô hợp lệ, vùng trồng active (Vùng trồng Lúa 04)
INSERT IGNORE INTO production_lot
    (id, organization_id, farm_area_id, product_category_id, name, expected_quantity,
     expected_quantity_unit, actual_quantity, planting_date, harvest_date, status,
     approval_notes, created_by, approved_by, created_at, updated_at)
VALUES
    ('c0000000-0000-0000-0000-000200000001', @org_htx,
     '00000000-0000-0000-0000-000100000004', @cat_lua,
     'TEST_NCL02_CN007_SOURCE_HAPPY_2026', 3000, 'kg', 2950,
     '2025-02-15', '2026-05-20', 'APPROVED',
     'Lô mẫu test happy path NCL-02-CN-007', @user_orgmanager, @user_orgmanager,
     NOW(), NOW());

-- 2.2 EXPIRED SOURCE: chứng nhận gồm valid + expired + rejected (Vùng trồng Chè 10)
INSERT IGNORE INTO production_lot
    (id, organization_id, farm_area_id, product_category_id, name, expected_quantity,
     expected_quantity_unit, actual_quantity, planting_date, harvest_date, status,
     approval_notes, created_by, approved_by, created_at, updated_at)
VALUES
    ('c0000000-0000-0000-0000-000200000002', @org_htx,
     '00000000-0000-0000-0000-000100000010', @cat_che,
     'TEST_NCL02_CN007_SOURCE_EXPIRED_CERT', 2000, 'kg', 1950,
     '2025-03-01', '2026-04-15', 'APPROVED',
     'Lô mẫu test chứng nhận hết hạn NCL-02-CN-007', @user_orgmanager, @user_orgmanager,
     NOW(), NOW());

-- 2.3 INACTIVE SOURCE: thuộc vùng trồng is_active = 0 cho TC-07
INSERT IGNORE INTO production_lot
    (id, organization_id, farm_area_id, product_category_id, name, expected_quantity,
     expected_quantity_unit, actual_quantity, planting_date, harvest_date, status,
     approval_notes, created_by, approved_by, created_at, updated_at)
VALUES
    ('c0000000-0000-0000-0000-000200000003', @org_htx,
     'c0000000-0000-0000-0000-000100000002', @cat_lua,
     'TEST_NCL02_CN007_SOURCE_INACTIVE_AREA', 1500, 'kg', NULL,
     '2025-04-01', NULL, 'DRAFT',
     'Lô mẫu test vùng trồng ngừng sử dụng NCL-02-CN-007', @user_orgmanager, NULL,
     NOW(), NOW());

-- 2.4 HISTORY SOURCE: có shipment/trace code/farm log/chain event/activity log
INSERT IGNORE INTO production_lot
    (id, organization_id, farm_area_id, product_category_id, name, expected_quantity,
     expected_quantity_unit, actual_quantity, planting_date, harvest_date, status,
     approval_notes, created_by, approved_by, created_at, updated_at)
VALUES
    ('c0000000-0000-0000-0000-000200000004', @org_htx,
     'c0000000-0000-0000-0000-000100000001', @cat_lua,
     'TEST_NCL02_CN007_SOURCE_WITH_HISTORY', 2500, 'kg', 2400,
     '2025-01-10', '2026-06-30', 'PACKAGED',
     'Lô mẫu test history isolation NCL-02-CN-007', @user_orgmanager, @user_orgmanager,
     NOW(), NOW());

-- 2.5 OTHER_ORG SOURCE: thuộc DEMO_NSV cho TC-08
INSERT IGNORE INTO production_lot
    (id, organization_id, farm_area_id, product_category_id, name, expected_quantity,
     expected_quantity_unit, actual_quantity, planting_date, harvest_date, status,
     approval_notes, created_by, approved_by, created_at, updated_at)
VALUES
    ('c0000000-0000-0000-0000-000200000005', @org_nsv,
     'c0000000-0000-0000-0000-000100000003', @cat_xoai,
     'TEST_NCL02_CN007_SOURCE_OTHER_ORG', 1000, 'kg', NULL,
     '2025-05-01', NULL, 'PACKAGED',
     'Lô mẫu test tổ chức khác NCL-02-CN-007', @user_procurement, NULL,
     NOW(), NOW());

-- ============================================================
-- 3. Certifications (chứng nhận test)
-- ============================================================
INSERT IGNORE INTO certifications
    (id, organization_id, standard_id, name, issuing_body, code, issued_by,
     issue_date, expiry_date, verification_status, created_at, updated_at)
VALUES
    -- 3.1 Chứng nhận còn hiệu lực cho HAPPY SOURCE (TC-05)
    ('c0000000-0000-0000-0000-000300000001', @org_htx, @std_vietgap,
     'TEST_NCL02_CN007_CERT_VALID_001', 'Tổng cục QLCL nông lâm thủy sản',
     'TEST-NCL02-VALID-001', 'Trung tâm chứng nhận chất lượng',
     DATE_ADD(CURDATE(), INTERVAL -180 DAY), DATE_ADD(CURDATE(), INTERVAL 365 DAY),
     'VERIFIED', NOW(), NOW()),
    -- 3.2 Chứng nhận còn hiệu lực cho EXPIRED SOURCE
    ('c0000000-0000-0000-0000-000300000002', @org_htx, @std_globalgap,
     'TEST_NCL02_CN007_CERT_VALID_002', 'Tổ chức chứng nhận GlobalG.A.P.',
     'TEST-NCL02-VALID-002', 'Trung tâm chứng nhận chất lượng',
     DATE_ADD(CURDATE(), INTERVAL -180 DAY), DATE_ADD(CURDATE(), INTERVAL 365 DAY),
     'VERIFIED', NOW(), NOW()),
    -- 3.3 Chứng nhận ĐÃ HẾT HẠN cho EXPIRED SOURCE (TC-06)
    ('c0000000-0000-0000-0000-000300000003', @org_htx, @std_vietgap,
     'TEST_NCL02_CN007_CERT_EXPIRED_001', 'Tổng cục QLCL nông lâm thủy sản',
     'TEST-NCL02-EXPIRED-001', 'Trung tâm chứng nhận chất lượng',
     DATE_ADD(CURDATE(), INTERVAL -400 DAY), DATE_ADD(CURDATE(), INTERVAL -30 DAY),
     'VERIFIED', NOW(), NOW()),
    -- 3.4 Chứng nhận BỊ TỪ CHỐI xác thực cho EXPIRED SOURCE (TC-06b)
    ('c0000000-0000-0000-0000-000300000004', @org_htx, @std_vietgap,
     'TEST_NCL02_CN007_CERT_REJECTED_001', 'Tổng cục QLCL nông lâm thủy sản',
     'TEST-NCL02-REJECTED-001', 'Trung tâm chứng nhận chất lượng',
     DATE_ADD(CURDATE(), INTERVAL -90 DAY), DATE_ADD(CURDATE(), INTERVAL 365 DAY),
     'REJECTED', NOW(), NOW()),
    -- 3.5 Chứng nhận còn hiệu lực cho HISTORY SOURCE
    ('c0000000-0000-0000-0000-000300000005', @org_htx, @std_vietgap,
     'TEST_NCL02_CN007_CERT_VALID_003', 'Tổng cục QLCL nông lâm thủy sản',
     'TEST-NCL02-VALID-003', 'Trung tâm chứng nhận chất lượng',
     DATE_ADD(CURDATE(), INTERVAL -120 DAY), DATE_ADD(CURDATE(), INTERVAL 365 DAY),
     'VERIFIED', NOW(), NOW());

-- ============================================================
-- 4. Gắn chứng nhận vào lô (production_lot_certifications)
-- ============================================================
INSERT IGNORE INTO production_lot_certifications
    (id, production_lot_id, certification_id, attached_at, attached_by, note)
VALUES
    ('c0000000-0000-0000-0000-000400000001',
     'c0000000-0000-0000-0000-000200000001',   -- HAPPY
     'c0000000-0000-0000-0000-000300000001',
     NOW(), @user_orgmanager, 'Chứng nhận test HAPPY'),
    ('c0000000-0000-0000-0000-000400000002',
     'c0000000-0000-0000-0000-000200000002',   -- EXPIRED -> valid
     'c0000000-0000-0000-0000-000300000002',
     NOW(), @user_orgmanager, 'Chứng nhận còn hiệu lực'),
    ('c0000000-0000-0000-0000-000400000003',
     'c0000000-0000-0000-0000-000200000002',   -- EXPIRED -> expired
     'c0000000-0000-0000-0000-000300000003',
     NOW(), @user_orgmanager, 'Chứng nhận hết hạn'),
    ('c0000000-0000-0000-0000-000400000004',
     'c0000000-0000-0000-0000-000200000002',   -- EXPIRED -> rejected
     'c0000000-0000-0000-0000-000300000004',
     NOW(), @user_orgmanager, 'Chứng nhận bị từ chối xác thực'),
    ('c0000000-0000-0000-0000-000400000005',
     'c0000000-0000-0000-0000-000200000004',   -- HISTORY
     'c0000000-0000-0000-0000-000300000005',
     NOW(), @user_orgmanager, 'Chứng nhận test HISTORY');

-- ============================================================
-- 5. History cho HISTORY SOURCE (TC-04/TC-11)
-- ============================================================
-- 5.1 Shipment
INSERT IGNORE INTO shipments
    (id, production_lot_id, organization_id, name, total_quantity, packaging_info,
     status, created_by, created_at, updated_at, code_range_id)
VALUES
    ('c0000000-0000-0000-0000-000500000001',
     'c0000000-0000-0000-0000-000200000004', @org_htx,
     'TEST_NCL02_CN007_SHIPMENT_HISTORY', 2400, 'Bao 20kg',
     'ACTIVATED', @user_orgmanager, NOW(), NOW(), NULL);

-- 5.2 Trace codes
INSERT IGNORE INTO trace_codes
    (id, shipment_id, code_value, qr_image, status, activated_at, activated_by, created_at)
VALUES
    ('c0000000-0000-0000-0000-000600000001',
     'c0000000-0000-0000-0000-000500000001', 'TEST-NCL02-TRACE-0001', NULL,
     'ACTIVE', NOW(), @user_orgmanager, NOW()),
    ('c0000000-0000-0000-0000-000600000002',
     'c0000000-0000-0000-0000-000500000001', 'TEST-NCL02-TRACE-0002', NULL,
     'INACTIVE', NULL, NULL, NOW());

-- 5.3 Farm log (nhật ký canh tác)
INSERT IGNORE INTO farm_logs
    (id, production_lot_id, activity_type, material, quantity, unit, executed_date,
     notes, created_by, created_at)
VALUES
    ('c0000000-0000-0000-0000-000700000001',
     'c0000000-0000-0000-0000-000200000004', 'PLANTING', 'Giống lúa nếp', 120, 'kg',
     '2025-01-10', 'Test farm log lô nguồn NCL-02-CN-007', @user_orgmanager, NOW());

-- 5.4 Chain event (sự kiện chuỗi)
INSERT IGNORE INTO chain_events
    (id, shipment_id, event_type, event_data, location, recorded_at, recorded_by,
     recorded_organization_id, created_at, parent_event_id, is_correction, hash, previous_hash)
VALUES
    ('c0000000-0000-0000-0000-000800000001',
     'c0000000-0000-0000-0000-000500000001', 'HARVESTING',
     JSON_OBJECT('quantity', 2400, 'note', 'Test chain event NCL-02-CN-007'),
     ST_GeomFromText('POINT(105.8804 21.0604)'), NOW(), @user_orgmanager, @org_htx,
     NOW(), NULL, 0, NULL, NULL);

-- 5.5 Activity log (lịch sử hoạt động) gắn lô nguồn
INSERT IGNORE INTO activity_logs
    (id, organization_id, user_id, username, full_name, action, description,
     entity_type, entity_id, ip_address, created_at)
VALUES
    ('c0000000-0000-0000-0000-000900000001', @org_htx, @user_orgmanager,
     'orgmanager', 'Quản lý HTX Demo (VT-02)', 'CREATE',
     'Test activity log lô nguồn NCL-02-CN-007',
     'ProductionLot', 'c0000000-0000-0000-0000-000200000004', NULL, NOW());

-- ============================================================
-- 6. Kiểm tra nhanh
-- ============================================================
SELECT 'seed-done' AS status;

SELECT id, name, status
FROM production_lot
WHERE id LIKE 'c0000000-0000-0000-0000-0002%';