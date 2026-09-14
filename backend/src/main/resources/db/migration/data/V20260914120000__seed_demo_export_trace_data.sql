-- ============================================================
-- V20260914120000: Seed data for English trace lookup and Admin testing (US NCL-06-CN-004)
-- 
-- Thêm dữ liệu mẫu hoàn chỉnh gồm:
-- 1. Tên tiếng Anh (name_en) cho Danh mục nông sản, Tiêu chuẩn chất lượng & Chỉ tiêu kiểm nghiệm
-- 2. Vùng trồng, Lô sản xuất xuất khẩu (EXPORT_QUALIFIED)
-- 3. Chứng nhận GlobalG.A.P., VietGAP đã xác thực (VERIFIED)
-- 4. Lô hàng xuất khẩu (shipments) và các mã tem truy xuất:
--    - DEMO-MANGO-EN-001 (ACTIVE)
--    - DEMO-MANGO-EN-002 (ACTIVE)
--    - DEMO-MANGO-EN-003 (ACTIVE)
--    - DEMO-MANGO-EN-004 (LOCKED)
--    - DEMO-MANGO-EN-005 (RECALLED)
-- 5. Chuỗi sự kiện nhật ký chuỗi cung ứng (chain_events) để hiển thị Timeline và Bản đồ đường đi (Route Map)
-- ============================================================

-- 1. Cập nhật name_en cho Danh mục nông sản đã có & thêm mới dữ liệu mẫu
INSERT INTO product_categories (id, name, name_en, category_group, description, is_active) VALUES
('55c91bdb-9df8-4410-a688-b0bada1cff40', 'Xoài Cát Chu', 'Cat Chu Mango', 'Cây ăn quả', 'Xoài Cát Chu Cao Lãnh Đồng Tháp chuyên xuất khẩu', TRUE),
('9703eb68-6ea5-43c6-88eb-f8449aae0b14', 'Vải thiều Thanh Hà', 'Thanh Ha Lychee', 'Cây ăn quả', 'Vải thiều đặc sản Hải Dương xuất khẩu', TRUE),
('88888888-0000-0000-0000-000000000001', 'Bưởi Phúc Trạch', 'Phuc Trach Pomelo', 'Cây ăn quả', 'Bưởi Phúc Trạch đặc sản Hà Tĩnh xuất khẩu', TRUE),
('88888888-0000-0000-0000-000000000002', 'Thanh long ruột đỏ', 'Red Flesh Dragon Fruit', 'Cây ăn quả', 'Thanh long ruột đỏ Bình Thuận xuất khẩu', TRUE),
('88888888-0000-0000-0000-000000000003', 'Sầu riêng Ri6', 'Ri6 Durian', 'Cây ăn quả', 'Sầu riêng Ri6 Tây Nguyên xuất khẩu', TRUE),
('88888888-0000-0000-0000-000000000004', 'Cà phê Arabica', 'Arabica Coffee Beans', 'Nông sản chế biến', 'Cà phê Arabica Cầu Đất Đà Lạt', TRUE),
('88888888-0000-0000-0000-000000000005', 'Trà Oolong', 'Oolong Tea', 'Nông sản chế biến', 'Trà Oolong Bảo Lộc Lâm Đồng', TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    name_en = VALUES(name_en),
    category_group = VALUES(category_group),
    description = VALUES(description),
    is_active = VALUES(is_active);

-- 2. Cập nhật name_en cho các Tiêu chuẩn chất lượng
UPDATE standards SET name_en = 'VietGAP' WHERE name = 'VietGAP';
UPDATE standards SET name_en = 'GlobalG.A.P. Standard' WHERE name = 'GlobalG.A.P.';
UPDATE standards SET name_en = 'HACCP Food Safety Standard' WHERE name = 'HACCP (TCVN 5603)';
UPDATE standards SET name_en = 'ISO 22000:2018 FSMS' WHERE name = 'ISO 22000:2018';
UPDATE standards SET name_en = 'FSSC 22000 Certification' WHERE name = 'FSSC 22000';
UPDATE standards SET name_en = 'TCVN 11041-2 Organic Cultivation' WHERE name = 'TCVN 11041-2:2017';
UPDATE standards SET name_en = 'USDA Organic Standard' WHERE name = 'USDA Organic';
UPDATE standards SET name_en = 'EU Organic Standard (EU 2018/848)' WHERE name = 'EU Organic';
UPDATE standards SET name_en = 'Rainforest Alliance Certified' WHERE name = 'Rainforest Alliance';
UPDATE standards SET name_en = 'Fairtrade International' WHERE name = 'Fairtrade';
UPDATE standards SET name_en = 'BRCGS Global Food Safety Standard' WHERE name = 'BRCGS Food Safety';
UPDATE standards SET name_en = 'IFS Food Standard' WHERE name = 'IFS Food';
UPDATE standards SET name_en = 'SQF Standard' WHERE name = 'SQF';
UPDATE standards SET name_en = 'ASEAN Good Agricultural Practices' WHERE name = 'ASEAN GAP';
UPDATE standards SET name_en = 'Codex Alimentarius (Fresh Produce)' WHERE name = 'Codex Alimentarius (rau quả tươi)';

-- 3. Cập nhật name_en cho các Chỉ tiêu kiểm nghiệm phổ biến
UPDATE inspection_criterion_catalog SET name_en = 'Chlorpyrifos Ethyl Residue' WHERE name = 'Dư lượng Chlorpyrifos ethyl' OR name = 'Dư lượng Chlorpyrifos';
UPDATE inspection_criterion_catalog SET name_en = 'Cypermethrin Residue' WHERE name = 'Dư lượng Cypermethrin';
UPDATE inspection_criterion_catalog SET name_en = 'Lambda-cyhalothrin Residue' WHERE name = 'Dư lượng Lambda-cyhalothrin';
UPDATE inspection_criterion_catalog SET name_en = 'Carbendazim Residue' WHERE name = 'Dư lượng Carbendazim';
UPDATE inspection_criterion_catalog SET name_en = 'Fipronil Residue' WHERE name = 'Dư lượng Fipronil';
UPDATE inspection_criterion_catalog SET name_en = 'Lead (Pb) Content' WHERE name = 'Hàm lượng Chì (Pb)';
UPDATE inspection_criterion_catalog SET name_en = 'Cadmium (Cd) Content' WHERE name = 'Hàm lượng Cadmi (Cd)';
UPDATE inspection_criterion_catalog SET name_en = 'Arsenic (As) Content' WHERE name = 'Hàm lượng Asen (As)' OR name = 'Hàm lượng Asen tổng số';
UPDATE inspection_criterion_catalog SET name_en = 'Mercury (Hg) Content' WHERE name = 'Hàm lượng Thủy ngân (Hg)';
UPDATE inspection_criterion_catalog SET name_en = 'Nitrate (NO3-) Residue' WHERE name = 'Dư lượng Nitrat (NO3-)';
UPDATE inspection_criterion_catalog SET name_en = 'Total Coliforms' WHERE name = 'Coliform tổng số';
UPDATE inspection_criterion_catalog SET name_en = 'Escherichia coli' WHERE name = 'E. coli';
UPDATE inspection_criterion_catalog SET name_en = 'Total Yeast and Mold' WHERE name = 'Tổng số nấm men, nấm mốc';
UPDATE inspection_criterion_catalog SET name_en = 'Imidacloprid Residue' WHERE name = 'Dư lượng Imidacloprid';
UPDATE inspection_criterion_catalog SET name_en = 'Glyphosate Residue' WHERE name = 'Dư lượng Glyphosate';

-- 4. Tạo Vùng trồng mẫu
INSERT IGNORE INTO farm_areas (id, organization_id, crop_type, name, area, area_unit, location, is_active, created_at, updated_at)
VALUES (
    '77777777-0000-0000-0000-000000000001',
    '763b3482-b96e-49cb-b37c-5c43c6126452',
    '55c91bdb-9df8-4410-a688-b0bada1cff40',
    'Vùng trồng Xoài Cát Chu Cao Lãnh #01 (Mã số: VN-DTP-088)',
    12.50,
    'ha',
    ST_GeomFromText('POINT(105.6321 10.4578)'),
    TRUE,
    NOW(),
    NOW()
);

-- 5. Tạo Chứng nhận GlobalG.A.P. đã xác thực cho tổ chức
INSERT IGNORE INTO certifications (id, organization_id, standard_id, name, issuing_body, code, issued_by, issue_date, expiry_date, verification_status, created_at, updated_at)
VALUES (
    '77777777-0000-0000-0000-000000000002',
    '763b3482-b96e-49cb-b37c-5c43c6126452',
    '9e24816d-b3e5-4531-b990-2d2f0432f1f3',
    'Chứng nhận GlobalG.A.P. Xoài Cát Chu Xuất Khẩu',
    'GLOBALG.A.P.',
    'GLOBALGAP-DTP-2026-09',
    'SGS Vietnam Co., Ltd.',
    '2026-01-15',
    '2027-01-14',
    'VERIFIED',
    NOW(),
    NOW()
);

-- 6. Tạo Lô sản xuất xuất khẩu mẫu
INSERT IGNORE INTO production_lot (id, organization_id, farm_area_id, product_category_id, name, expected_quantity, expected_quantity_unit, actual_quantity, planting_date, harvest_date, status, created_by, approved_by, created_at, updated_at)
VALUES (
    '77777777-0000-0000-0000-000000000003',
    '763b3482-b96e-49cb-b37c-5c43c6126452',
    '77777777-0000-0000-0000-000000000001',
    '55c91bdb-9df8-4410-a688-b0bada1cff40',
    'Lô Xoài Cát Chu Cao Lãnh Xuất Khẩu Thị Trường Hoa Kỳ',
    5000.0,
    'kg',
    4850.0,
    '2026-03-01',
    '2026-08-25',
    'EXPORT_QUALIFIED',
    'b5796413-179b-42d9-988c-dab8a5f42ed6',
    'd9b25f5d-b02c-11f1-ae6b-eecfef049056',
    NOW(),
    NOW()
);

-- 7. Tạo Lô hàng xuất khẩu mẫu
INSERT IGNORE INTO shipments (id, production_lot_id, organization_id, name, total_quantity, packaging_info, status, created_by, created_at, updated_at)
VALUES (
    '77777777-0000-0000-0000-000000000004',
    '77777777-0000-0000-0000-000000000003',
    '763b3482-b96e-49cb-b37c-5c43c6126452',
    'Lô hàng xuất khẩu Xoài Cát Chu sang Cảng Long Beach, Hoa Kỳ (Mã lô: US-MANGO-2026-01)',
    450,
    'Thùng carton chuyên dụng 10kg, bọc màng khí hút chân không',
    'ACTIVATED',
    'b5796413-179b-42d9-988c-dab8a5f42ed6',
    NOW(),
    NOW()
);

-- 8. Tạo Các mã tem truy xuất phục vụ test tra cứu tiếng Anh (English Trace Lookup Test Codes)
-- Mã 1: ACTIVE - Hàng đang lưu thông bình thường
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, activated_by)
VALUES (
    '77777777-0000-0001-0000-000000000001',
    '77777777-0000-0000-0000-000000000004',
    'DEMO-MANGO-EN-001',
    'ACTIVE',
    NOW(), NOW(), NOW(),
    'b5796413-179b-42d9-988c-dab8a5f42ed6'
);

-- Mã 2: ACTIVE
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, activated_by)
VALUES (
    '77777777-0000-0001-0000-000000000002',
    '77777777-0000-0000-0000-000000000004',
    'DEMO-MANGO-EN-002',
    'ACTIVE',
    NOW(), NOW(), NOW(),
    'b5796413-179b-42d9-988c-dab8a5f42ed6'
);

-- Mã 3: ACTIVE
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, activated_by)
VALUES (
    '77777777-0000-0001-0000-000000000003',
    '77777777-0000-0000-0000-000000000004',
    'DEMO-MANGO-EN-003',
    'ACTIVE',
    NOW(), NOW(), NOW(),
    'b5796413-179b-42d9-988c-dab8a5f42ed6'
);

-- Mã 4: LOCKED - Mã bị tạm khóa do nghi vấn bất thường
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, activated_by, locked_at, locked_by, lock_reason)
VALUES (
    '77777777-0000-0001-0000-000000000004',
    '77777777-0000-0000-0000-000000000004',
    'DEMO-MANGO-EN-004',
    'LOCKED',
    NOW(), NOW(), NOW(),
    'b5796413-179b-42d9-988c-dab8a5f42ed6',
    NOW(),
    'd9b25f5d-b02c-11f1-ae6b-eecfef049056',
    'Tạm khóa tem do phát hiện quét đồng thời ở 2 tọa độ cách xa nhau'
);

-- Mã 5: RECALLED - Mã nằm trong lô hàng bị thu hồi
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, activated_by)
VALUES (
    '77777777-0000-0001-0000-000000000005',
    '77777777-0000-0000-0000-000000000004',
    'DEMO-MANGO-EN-005',
    'RECALLED',
    NOW(), NOW(), NOW(),
    'b5796413-179b-42d9-988c-dab8a5f42ed6'
);

-- 9. Chuỗi sự kiện nhật ký chuỗi cung ứng (chain_events)
INSERT IGNORE INTO chain_events (id, shipment_id, event_type, event_data, location, recorded_at, recorded_by, recorded_organization_id, created_at)
VALUES 
(
    '77777777-0000-0002-0000-000000000001',
    '77777777-0000-0000-0000-000000000004',
    'PLANTING',
    '{"title": "Xuống giống cây Xoài Cát Chu", "description": "Xuống giống xoài Cát Chu ghép cành chất lượng cao tại vườn Cao Lãnh", "actor": "Nguyễn Văn Hùng - Chủ vườn"}',
    ST_GeomFromText('POINT(105.6321 10.4578)'),
    '2026-03-01 08:00:00',
    'b5796413-179b-42d9-988c-dab8a5f42ed6',
    '763b3482-b96e-49cb-b37c-5c43c6126452',
    NOW()
),
(
    '77777777-0000-0002-0000-000000000002',
    '77777777-0000-0000-0000-000000000004',
    'CULTIVATION',
    '{"title": "Bón phân hữu cơ & Bao trái xoài", "description": "Bón phân hữu cơ sinh học vi sinh và tiến hành bao bọc trái bảo vệ khỏi sâu bệnh theo tiêu chuẩn GlobalG.A.P.", "actor": "Tổ canh tác số 2 - HTX GGJ"}',
    ST_GeomFromText('POINT(105.6321 10.4578)'),
    '2026-05-15 09:30:00',
    'b5796413-179b-42d9-988c-dab8a5f42ed6',
    '763b3482-b96e-49cb-b37c-5c43c6126452',
    NOW()
),
(
    '77777777-0000-0002-0000-000000000003',
    '77777777-0000-0000-0000-000000000004',
    'HARVESTING',
    '{"title": "Thu hoạch xoài Cát Chu", "description": "Thu hoạch thủ công chọn lọc quả độ chín 85-90%, xếp sọt nhựa chuyên dụng phân loại tại vườn", "quantity": 4850, "unit": "kg"}',
    ST_GeomFromText('POINT(105.6321 10.4578)'),
    '2026-08-25 06:30:00',
    'b5796413-179b-42d9-988c-dab8a5f42ed6',
    '763b3482-b96e-49cb-b37c-5c43c6126452',
    NOW()
),
(
    '77777777-0000-0002-0000-000000000004',
    '77777777-0000-0000-0000-000000000004',
    'PREPROCESSING',
    '{"title": "Sơ chế & Xử lý nước nóng diệt vi sinh", "description": "Rửa sạch, xử lý diệt vi sinh bằng nước nóng 48°C trong 5 phút và làm mát hạ nhiệt", "facility": "Nhà sơ chế nông sản HTX GGJ"}',
    ST_GeomFromText('POINT(105.6350 10.4600)'),
    '2026-08-25 14:00:00',
    'b5796413-179b-42d9-988c-dab8a5f42ed6',
    '763b3482-b96e-49cb-b37c-5c43c6126452',
    NOW()
),
(
    '77777777-0000-0002-0000-000000000005',
    '77777777-0000-0000-0000-000000000004',
    'PACKAGING',
    '{"title": "Đóng gói & Dán tem truy xuất nguồn gốc", "description": "Đóng thùng carton 10kg, bọc túi màng khí và dán tem mã QR truy xuất nguồn gốc từng thùng", "totalPackages": 450}',
    ST_GeomFromText('POINT(105.6350 10.4600)'),
    '2026-08-26 10:00:00',
    'b5796413-179b-42d9-988c-dab8a5f42ed6',
    '763b3482-b96e-49cb-b37c-5c43c6126452',
    NOW()
),
(
    '77777777-0000-0002-0000-000000000006',
    '77777777-0000-0000-0000-000000000004',
    'SHIPPING',
    '{"title": "Vận chuyển bằng xe lạnh sang Cảng Cát Lái", "description": "Xếp hàng lên container lạnh 12°C vận chuyển từ Đồng Tháp về Cảng Cát Lái (TP.HCM)", "carrier": "Công ty Vận tải Container Lạnh Tiên Phong"}',
    ST_GeomFromText('POINT(106.7900 10.7600)'),
    '2026-08-27 16:30:00',
    'b5796413-179b-42d9-988c-dab8a5f42ed6',
    '763b3482-b96e-49cb-b37c-5c43c6126452',
    NOW()
);
