-- ============================================================
-- Seed: Yeni lô sản xuç PACKAGED đủ điều kiện tạo lô hàng
-- (NCL-04-CN-007 test dữ liție chuẩn bürü)
--
-- Amaç: kullanıcının "tạo lô hàng + sinh mã truy xuât" akışını
-- rahatça test etmesi için:
--   1) Dải mã mevcut (DEMO-HTX-01) hạn mük'ünü büyült.
--   2) 3 yeni lô sản xuç PACKAGED + inspection PASSED (3 VietGAP
--      kriteri, 2027'ye kadar geçerli) + HARVEST/PREPROCESSING/
--      PACKAGING chain events ekle (shipment YOK).
--
-- Idempotent: INSERT IGNORE + sabit ID'ler. DB: dev/demo.
-- Çalıştırma:
--   Get-Content docs\sample-data\seed_eligible_packaged_lots.sql |
--     docker exec -i nguon-goc-so-mysql-1 mysql -unguongocso -p29012005 nguon_goc_so
-- ============================================================

-- 0) Dải mã hạn mük'ünü rahatlat (gerçek trace_codes = 60, kalanı seed artığı)
UPDATE code_ranges
SET total_limit = 100000,
    used_count   = (SELECT COUNT(*) FROM trace_codes)
WHERE organization_id = '327a3a0e-a396-11f1-aea2-32ec817c7ea4'
  AND prefix = 'DEMO-HTX-01';

-- 1) Vùng tròn 16–18 (yeni lô'lar için farm_area)
INSERT IGNORE INTO farm_areas
    (id, organization_id, crop_type, name, area, area_unit, location, is_active, created_at, updated_at)
SELECT
    CONCAT('00000000-0000-0000-0000-0001', LPAD(t.i, 8, '0')),
    '327a3a0e-a396-11f1-aea2-32ec817c7ea4',
    (SELECT id FROM product_categories WHERE name = t.crop),
    CONCAT('Vùng tròn ', t.crop, ' ', LPAD(t.i, 2, '0')),
    t.area,
    'HA',
    ST_GeomFromText(CONCAT('POINT(', t.lon, ' ', t.lat, ')')),
    TRUE,
    NOW(),
    NOW()
FROM (
    SELECT 16 i, 'Cà phê' crop, 2.60 area, '105.9316' lon, '21.0316' lat
    UNION ALL SELECT 17, 'Chè', 3.80, '105.9417', '21.0517'
    UNION ALL SELECT 18, 'Nho', 2.40, '105.9518', '21.0718'
) t;

-- 2) Lô sản xuç 16–18 — PACKAGED
INSERT IGNORE INTO production_lot
    (id, organization_id, farm_area_id, product_category_id, name, expected_quantity,
     expected_quantity_unit, actual_quantity, planting_date, harvest_date, status,
     approval_notes, created_by, approved_by, created_at, updated_at)
SELECT
    CONCAT('00000000-0000-0000-0000-0002', LPAD(t.i, 8, '0')),
    '327a3a0e-a396-11f1-aea2-32ec817c7ea4',
    CONCAT('00000000-0000-0000-0000-0001', LPAD(t.i, 8, '0')),
    (SELECT id FROM product_categories WHERE name = t.crop),
    CONCAT('Lô ', t.crop, ' ', LPAD(t.i, 2, '0')),
    t.qty,
    'KG',
    t.qty,
    DATE_ADD(DATE '2026-07-01', INTERVAL t.i DAY),
    DATE_ADD(DATE '2026-09-05', INTERVAL t.i DAY),
    'PACKAGED',
    NULL,
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    NOW(),
    NOW()
FROM (
    SELECT 16 i, 'Cà phê' crop, 2400 qty
    UNION ALL SELECT 17, 'Chè', 2600
    UNION ALL SELECT 18, 'Nho', 1800
) t;