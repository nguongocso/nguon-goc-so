-- ============================================================
-- V52: Seed demo data cho role VT-02 (org DEMO_HTX)
--
-- Tạo dữ liệu demo cho tài khoản 'orgmanager' (VT-02) trong
-- tổ chức DEMO_HTX (seed ở V51):
--   - 8 danh mục sản phẩm (bắt buộc vì chưa có migration seed product_categories)
--   - 15 vùng trồng (farm_areas)
--   - 15 lô sản xuất (production_lot)
--   - 15 khóa API key bên thứ ba (partner_api_keys), raw key trả ở comment
--   - 15 chứng nhận (certifications) gắn 15 lô + 15 yêu cầu kiểm nghiệm
--     (inspection_requests) có chỉ tiêu + kết quả
--
-- Idempotent: tất cả ID cố định + INSERT IGNORE.
-- Tiêu chí/tham chiếu: standards (V49), inspection_criterion_catalog (V50),
-- tài khoản/org demo (V51).
-- ============================================================

-- 1. Product categories (cần thiết cho farm_areas.crop_type & production_lot.product_category_id)
INSERT IGNORE INTO product_categories
    (id, name, category_group, description, is_active, requires_inspection)
VALUES
('00000000-0000-0000-0000-000800000001', 'Nho', 'Cây ăn quả', 'Nho tươi ăn quả', TRUE, TRUE),
('00000000-0000-0000-0000-000800000002', 'Chè', 'Chè', 'Chè búp tươi', TRUE, TRUE),
('00000000-0000-0000-0000-000800000003', 'Xoài', 'Cây ăn quả', 'Xoài cát', TRUE, TRUE),
('00000000-0000-0000-0000-000800000004', 'Lúa', 'Cây lương thực', 'Lúa nếp', TRUE, TRUE),
('00000000-0000-0000-0000-000800000005', 'Sầu riêng', 'Cây ăn quả', 'Sầu riêng Ri6', TRUE, TRUE),
('00000000-0000-0000-0000-000800000006', 'Hồ tiêu', 'Cây gia vị', 'Tiêu đen', TRUE, TRUE),
('00000000-0000-0000-0000-000800000007', 'Rau cải', 'Rau', 'Cải xanh', TRUE, TRUE),
('00000000-0000-0000-0000-000800000008', 'Cà phê', 'Cây công nghiệp', 'Cà phê robusta', TRUE, TRUE);

-- 2. Farm areas (15 vùng trồng) thuộc DEMO_HTX
INSERT IGNORE INTO farm_areas
    (id, organization_id, crop_type, name, area, area_unit, location, is_active, created_at, updated_at)
SELECT
    CONCAT('00000000-0000-0000-0000-0001', LPAD(t.i, 8, '0')),
    (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX'),
    (SELECT id FROM product_categories WHERE name = t.crop),
    CONCAT('Vùng trồng ', t.crop, ' ', LPAD(t.i, 2, '0')),
    t.area,
    'HA',
    ST_GeomFromText(CONCAT('POINT(', t.lon, ' ', t.lat, ')')),
    TRUE,
    NOW(),
    NOW()
FROM (
    SELECT 1 i, 'Nho' crop, 2.50 area, '105.8501' lon, '21.0301' lat
    UNION ALL SELECT 2, 'Chè', 3.20, '105.8602', '21.0402'
    UNION ALL SELECT 3, 'Xoài', 1.80, '105.8703', '21.0503'
    UNION ALL SELECT 4, 'Lúa', 5.00, '105.8804', '21.0604'
    UNION ALL SELECT 5, 'Sầu riêng', 4.10, '105.8905', '21.0705'
    UNION ALL SELECT 6, 'Hồ tiêu', 1.20, '105.9006', '21.0206'
    UNION ALL SELECT 7, 'Rau cải', 0.80, '105.9107', '21.0407'
    UNION ALL SELECT 8, 'Cà phê', 6.40, '105.9208', '21.0608'
    UNION ALL SELECT 9, 'Nho', 2.20, '105.9309', '21.0309'
    UNION ALL SELECT 10, 'Chè', 3.60, '105.9410', '21.0510'
    UNION ALL SELECT 11, 'Xoài', 2.90, '105.9511', '21.0711'
    UNION ALL SELECT 12, 'Lúa', 4.50, '105.9612', '21.0212'
    UNION ALL SELECT 13, 'Sầu riêng', 3.00, '105.9713', '21.0613'
    UNION ALL SELECT 14, 'Hồ tiêu', 1.50, '105.9814', '21.0414'
    UNION ALL SELECT 15, 'Rau cải', 0.90, '105.9915', '21.0815'
) t;

-- 3. Production lots (15 lô sản xuất) - 1 vùng trồng / lô
INSERT IGNORE INTO production_lot
    (id, organization_id, farm_area_id, product_category_id, name, expected_quantity,
     expected_quantity_unit, actual_quantity, planting_date, harvest_date, status,
     approval_notes, created_by, approved_by, created_at, updated_at)
SELECT
    CONCAT('00000000-0000-0000-0000-0002', LPAD(t.i, 8, '0')),
    (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX'),
    CONCAT('00000000-0000-0000-0000-0001', LPAD(t.i, 8, '0')),
    (SELECT id FROM product_categories WHERE name = t.crop),
    CONCAT('Lô ', t.crop, ' ', LPAD(t.i, 2, '0')),
    1500 + t.i * 300,
    'kg',
    1500 + t.i * 300 - 30,
    DATE_ADD(DATE '2025-02-01', INTERVAL 10 * t.i DAY),
    CASE WHEN t.status = 'APPROVED' THEN NULL
         ELSE DATE_ADD(DATE '2026-06-01', INTERVAL 5 * t.i DAY)
    END,
    t.status,
    'Lô demo seed V52',
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    NOW(),
    NOW()
FROM (
    SELECT 1 i, 'Nho' crop, 'PACKAGED' status
    UNION ALL SELECT 2, 'Chè', 'PACKAGED'
    UNION ALL SELECT 3, 'Xoài', 'PACKAGED'
    UNION ALL SELECT 4, 'Lúa', 'PACKAGED'
    UNION ALL SELECT 5, 'Sầu riêng', 'PACKAGED'
    UNION ALL SELECT 6, 'Hồ tiêu', 'HARVESTED'
    UNION ALL SELECT 7, 'Rau cải', 'HARVESTED'
    UNION ALL SELECT 8, 'Cà phê', 'HARVESTED'
    UNION ALL SELECT 9, 'Nho', 'HARVESTED'
    UNION ALL SELECT 10, 'Chè', 'HARVESTED'
    UNION ALL SELECT 11, 'Xoài', 'APPROVED'
    UNION ALL SELECT 12, 'Lúa', 'APPROVED'
    UNION ALL SELECT 13, 'Sầu riêng', 'APPROVED'
    UNION ALL SELECT 14, 'Hồ tiêu', 'APPROVED'
    UNION ALL SELECT 15, 'Rau cải', 'APPROVED'
) t;

-- 4. Certifications (15 chứng nhận) cho 15 lô
INSERT IGNORE INTO certifications
    (id, organization_id, standard_id, name, issuing_body, code, issued_by,
     issue_date, expiry_date, created_at, updated_at)
SELECT
    CONCAT('00000000-0000-0000-0000-0003', LPAD(t.i, 8, '0')),
    (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX'),
    (SELECT id FROM standards WHERE name = t.standard_name),
    CONCAT('Chứng nhận ', t.standard_name, ' - Lô ', LPAD(t.i, 2, '0')),
    (SELECT issuing_body FROM standards WHERE name = t.standard_name),
    CONCAT('CERT-DEMOHTX-', LPAD(t.i, 3, '0')),
    'Tổng cục Quản lý chất lượng nông, lâm, thủy sản',
    DATE_ADD(DATE '2025-08-01', INTERVAL t.i DAY),
    DATE_ADD(DATE '2027-08-01', INTERVAL t.i DAY),
    NOW(),
    NOW()
FROM (
    SELECT 1 i, 'VietGAP' standard_name
    UNION ALL SELECT 2, 'GlobalG.A.P.'
    UNION ALL SELECT 3, 'HACCP (TCVN 5603)'
    UNION ALL SELECT 4, 'ISO 22000:2018'
    UNION ALL SELECT 5, 'FSSC 22000'
    UNION ALL SELECT 6, 'TCVN 11041-2:2017'
    UNION ALL SELECT 7, 'USDA Organic'
    UNION ALL SELECT 8, 'EU Organic'
    UNION ALL SELECT 9, 'Rainforest Alliance'
    UNION ALL SELECT 10, 'Fairtrade'
    UNION ALL SELECT 11, 'BRCGS Food Safety'
    UNION ALL SELECT 12, 'IFS Food'
    UNION ALL SELECT 13, 'SQF'
    UNION ALL SELECT 14, 'ASEAN GAP'
    UNION ALL SELECT 15, 'Codex Alimentarius (rau quả tươi)'
) t;

-- 5. Gắn chứng nhận vào lô (production_lot_certifications)
INSERT IGNORE INTO production_lot_certifications
    (id, production_lot_id, certification_id, attached_at, attached_by, note)
SELECT
    CONCAT('00000000-0000-0000-0000-0004', LPAD(t.i, 8, '0')),
    CONCAT('00000000-0000-0000-0000-0002', LPAD(t.i, 8, '0')),
    CONCAT('00000000-0000-0000-0000-0003', LPAD(t.i, 8, '0')),
    NOW(),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    CONCAT('Chứng nhận gắn cho lô ', t.i)
FROM (
    SELECT 1 i UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
) t;

-- 6. Inspection requests (15 yêu cầu kiểm nghiệm) - 1 yêu cầu / lô
--    Lô 1-12: PASSED, lô 13-14: PENDING_RESULT, lô 15: CANCELLED
INSERT IGNORE INTO inspection_requests
    (id, production_lot_id, inspection_unit, sample_sent_date, status, created_by, created_at, updated_at)
SELECT
    CONCAT('00000000-0000-0000-0000-0005', LPAD(t.i, 8, '0')),
    CONCAT('00000000-0000-0000-0000-0002', LPAD(t.i, 8, '0')),
    'Trung tâm Kiểm nghiệm Nông Lâm Thủy sản Miền Bắc',
    DATE_ADD(DATE '2026-01-15', INTERVAL t.i DAY),
    t.status,
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    NOW(),
    NOW()
FROM (
    SELECT 1 i, 'PASSED' status
    UNION ALL SELECT 2, 'PASSED'
    UNION ALL SELECT 3, 'PASSED'
    UNION ALL SELECT 4, 'PASSED'
    UNION ALL SELECT 5, 'PASSED'
    UNION ALL SELECT 6, 'PASSED'
    UNION ALL SELECT 7, 'PASSED'
    UNION ALL SELECT 8, 'PASSED'
    UNION ALL SELECT 9, 'PASSED'
    UNION ALL SELECT 10, 'PASSED'
    UNION ALL SELECT 11, 'PASSED'
    UNION ALL SELECT 12, 'PASSED'
    UNION ALL SELECT 13, 'PENDING_RESULT'
    UNION ALL SELECT 14, 'PENDING_RESULT'
    UNION ALL SELECT 15, 'CANCELLED'
) t;

-- 7. Chỉ tiêu kiểm nghiệm (3 chỉ tiêu VietGAP / yêu cầu kiểm nghiệm 1-14)
--    Tham chiếu danh mục chỉ tiêu đã seed ở V50.
INSERT IGNORE INTO inspection_criteria
    (id, inspection_request_id, criterion_code, criterion_name, standard_id, criterion_id)
SELECT
    CONCAT('00000000-0000-0000-0000-0006', LPAD((r.rn - 1) * 3 + c.m, 8, '0')),
    CONCAT('00000000-0000-0000-0000-0005', LPAD(r.rn, 8, '0')),
    c.code,
    c.cname,
    (SELECT id FROM standards WHERE name = 'VietGAP'),
    c.cid
FROM (
    SELECT 1 rn UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14
) r
CROSS JOIN (
    SELECT 1 m, 'HEAVY_METAL_PB' code, 'Hàm lượng Chì (Pb)' cname,
           (SELECT id FROM inspection_criterion_catalog
             WHERE name = 'Hàm lượng Chì (Pb)' AND reference_standard = 'VietGAP') cid
    UNION ALL
    SELECT 2, 'HEAVY_METAL_CD', 'Hàm lượng Cadmi (Cd)',
           (SELECT id FROM inspection_criterion_catalog
             WHERE name = 'Hàm lượng Cadmi (Cd)' AND reference_standard = 'VietGAP')
    UNION ALL
    SELECT 3, 'MICROBIO_E_COLI', 'E. coli',
           (SELECT id FROM inspection_criterion_catalog
             WHERE name = 'E. coli' AND reference_standard = 'VietGAP')
) c
WHERE r.rn <= 14;

-- 8. Kết quả kiểm nghiệm (cho yêu cầu PASSED 1-12, mỗi chỉ tiêu đều đạt)
INSERT IGNORE INTO inspection_criterion_results
    (id, inspection_criterion_id, result_date, expiry_date, passed, file_path, created_by, created_at, updated_at)
SELECT
    CONCAT('00000000-0000-0000-0000-0007', LPAD((r.rn - 1) * 3 + c.m, 8, '0')),
    CONCAT('00000000-0000-0000-0000-0006', LPAD((r.rn - 1) * 3 + c.m, 8, '0')),
    DATE_ADD(DATE '2026-02-01', INTERVAL (r.rn - 1) * 3 + c.m DAY),
    DATE_ADD(DATE '2027-02-01', INTERVAL (r.rn - 1) * 3 + c.m DAY),
    TRUE,
    NULL,
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    NOW(),
    NOW()
FROM (
    SELECT 1 rn UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12
) r
CROSS JOIN (SELECT 1 m UNION ALL SELECT 2 UNION ALL SELECT 3) c;

-- 9. Partner API Keys (15 khóa cho bên thứ ba)
--    key_hash = SHA2(raw_key, 256) khớp với hashSha256() trong PartnerApiKeyService.
--    RAW KEY (dùng header 'X-API-KEY') được ghi đầy đủ ở comment cuối file.
INSERT IGNORE INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, total_calls, failed_calls, created_by, created_at)
SELECT
    CONCAT('00000000-0000-0000-0000-0009', LPAD(t.i, 8, '0')),
    (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX'),
    CONCAT('Đối tác TTT-', LPAD(t.i, 2, '0')),
    CONCAT('nks_live_', SUBSTRING(t.hex, 1, 8)),
    SHA2(CONCAT('nks_live_', t.hex), 256),
    600 + t.i * 50,
    DATE_ADD(NOW(), INTERVAL 1 YEAR),
    'ACTIVE',
    0,
    0,
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    NOW()
FROM (
    SELECT 1 i, 'a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5' hex
    UNION ALL SELECT 2, 'a3b4c5d6a3b4c5d6a3b4c5d6a3b4c5d6a3b4c5d6a3b4c5d6a3b4c5d6a3b4c5d6'
    UNION ALL SELECT 3, 'a4b5c6d7a4b5c6d7a4b5c6d7a4b5c6d7a4b5c6d7a4b5c6d7a4b5c6d7a4b5c6d7'
    UNION ALL SELECT 4, 'a5b6c7d8a5b6c7d8a5b6c7d8a5b6c7d8a5b6c7d8a5b6c7d8a5b6c7d8a5b6c7d8'
    UNION ALL SELECT 5, 'a6b7c8d9a6b7c8d9a6b7c8d9a6b7c8d9a6b7c8d9a6b7c8d9a6b7c8d9a6b7c8d9'
    UNION ALL SELECT 6, 'a7b8c9daa7b8c9daa7b8c9daa7b8c9daa7b8c9daa7b8c9daa7b8c9daa7b8c9da'
    UNION ALL SELECT 7, 'a8b9cadba8b9cadba8b9cadba8b9cadba8b9cadba8b9cadba8b9cadba8b9cadb'
    UNION ALL SELECT 8, 'a9bacbdca9bacbdca9bacbdca9bacbdca9bacbdca9bacbdca9bacbdca9bacbdc'
    UNION ALL SELECT 9, 'aabbccddaabbccddaabbccddaabbccddaabbccddaabbccddaabbccddaabbccdd'
    UNION ALL SELECT 10, 'abbccddeabbccddeabbccddeabbccddeabbccddeabbccddeabbccddeabbccdde'
    UNION ALL SELECT 11, 'acbdcedfacbdcedfacbdcedfacbdcedfacbdcedfacbdcedfacbdcedfacbdcedf'
    UNION ALL SELECT 12, 'adbecfe0adbecfe0adbecfe0adbecfe0adbecfe0adbecfe0adbecfe0adbecfe0'
    UNION ALL SELECT 13, 'aebfd0e1aebfd0e1aebfd0e1aebfd0e1aebfd0e1aebfd0e1aebfd0e1aebfd0e1'
    UNION ALL SELECT 14, 'afc0d1e2afc0d1e2afc0d1e2afc0d1e2afc0d1e2afc0d1e2afc0d1e2afc0d1e2'
    UNION ALL SELECT 15, 'b0c1d2e3b0c1d2e3b0c1d2e3b0c1d2e3b0c1d2e3b0c1d2e3b0c1d2e3b0c1d2e3'
) t;

-- ============================================================
-- RAW API KEYS (dùng header X-API-KEY khi gọi public API)
--  1) nks_live_a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5
--  2) nks_live_a3b4c5d6a3b4c5d6a3b4c5d6a3b4c5d6a3b4c5d6a3b4c5d6a3b4c5d6a3b4c5d6
--  3) nks_live_a4b5c6d7a4b5c6d7a4b5c6d7a4b5c6d7a4b5c6d7a4b5c6d7a4b5c6d7a4b5c6d7
--  4) nks_live_a5b6c7d8a5b6c7d8a5b6c7d8a5b6c7d8a5b6c7d8a5b6c7d8a5b6c7d8a5b6c7d8
--  5) nks_live_a6b7c8d9a6b7c8d9a6b7c8d9a6b7c8d9a6b7c8d9a6b7c8d9a6b7c8d9a6b7c8d9
--  6) nks_live_a7b8c9daa7b8c9daa7b8c9daa7b8c9daa7b8c9daa7b8c9daa7b8c9daa7b8c9da
--  7) nks_live_a8b9cadba8b9cadba8b9cadba8b9cadba8b9cadba8b9cadba8b9cadba8b9cadb
--  8) nks_live_a9bacbdca9bacbdca9bacbdca9bacbdca9bacbdca9bacbdca9bacbdca9bacbdc
--  9) nks_live_aabbccddaabbccddaabbccddaabbccddaabbccddaabbccddaabbccddaabbccdd
-- 10) nks_live_abbccddeabbccddeabbccddeabbccddeabbccddeabbccddeabbccddeabbccdde
-- 11) nks_live_acbdcedfacbdcedfacbdcedfacbdcedfacbdcedfacbdcedfacbdcedfacbdcedf
-- 12) nks_live_adbecfe0adbecfe0adbecfe0adbecfe0adbecfe0adbecfe0adbecfe0adbecfe0
-- 13) nks_live_aebfd0e1aebfd0e1aebfd0e1aebfd0e1aebfd0e1aebfd0e1aebfd0e1aebfd0e1
-- 14) nks_live_afc0d1e2afc0d1e2afc0d1e2afc0d1e2afc0d1e2afc0d1e2afc0d1e2afc0d1e2
-- 15) nks_live_b0c1d2e3b0c1d2e3b0c1d2e3b0c1d2e3b0c1d2e3b0c1d2e3b0c1d2e3b0c1d2e3
-- ============================================================