
-- ============================================================
-- SEED DATA PHỤC VỤ KỊCH BẢN DEMO NGUỒN GỐC SỐ
-- Đầy đủ 5 Tab, Chuỗi băm SHA-256 INTACT, Bàn giao, Thu hồi, Quét bất thường
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Tổ chức Demo
INSERT INTO organizations (organization_id, name, code, type, status, address, phone, email, created_at, updated_at)
VALUES
('00000000-0000-0000-0000-000000000001', 'HTX Nông Sản Demo', 'DEMO_HTX', 'COOPERATIVE', 'ACTIVE', 'Thôn Thái An, Xã Vĩnh Hải, Huyện Ninh Hải, Ninh Thuận', '0912000031', 'demo-htx@nguongocso.test', NOW(), NOW()),
('00000000-0000-0000-0000-000000000010', 'Công ty Nông Sản Việt Demo', 'DEMO_NSV', 'ENTERPRISE', 'ACTIVE', 'Số 2 Đường Thu Mua, Quận 1, TP. Hồ Chí Minh', '0912000032', 'demo-nsv@nguongocso.test', NOW(), NOW()),
('00000000-0000-0000-0000-000000000020', 'Chi cục Quản lý Chất lượng Nông Sản', 'DEMO_GOV', 'GOVERNMENT', 'ACTIVE', 'Số 3 Đường Quản Lý, Ba Đình, Hà Nội', '0912000033', 'demo-gov@nguongocso.test', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 'ACTIVE';

-- 2. Tài khoản người dùng (mật khẩu mặc định: admin123)
-- Hash bcrypt $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2 = admin123
INSERT INTO users (user_id, user_name, password_hash, full_name, phone, email, status, created_at, updated_at)
VALUES
('00000000-0000-0000-0000-000000000002', 'orgmanager', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Quản lý HTX Demo (VT-02)', '0912000041', 'orgmanager@demo.test', 'ACTIVE', NOW(), NOW()),
('00000000-0000-0000-0000-000000000003', 'orgmanager2', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Phó Giám đốc HTX Demo (VT-02)', '0912000042', 'orgmanager2@demo.test', 'ACTIVE', NOW(), NOW()),
('00000000-0000-0000-0000-000000000004', 'eventrecorder', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Người ghi sự kiện Demo (VT-03)', '0912000043', 'eventrecorder@demo.test', 'ACTIVE', NOW(), NOW()),
('00000000-0000-0000-0000-000000000005', 'procurement', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Doanh nghiệp thu mua Demo (VT-04)', '0912000044', 'procurement@demo.test', 'ACTIVE', NOW(), NOW()),
('00000000-0000-0000-0000-000000000006', 'regulator', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Cán bộ Sở Nông nghiệp (VT-05)', '0912000045', 'regulator@demo.test', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = 'ACTIVE', password_hash = VALUES(password_hash), full_name = VALUES(full_name);

-- 3. Phân vai trò người dùng trong tổ chức (organization_users)
DELETE FROM organization_users WHERE user_id IN ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000006');

INSERT INTO organization_users (id, organization_id, user_id, role_id, custom_permissions, joined_at, status)
VALUES
(UUID(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 2, NULL, NOW(), 'ACTIVE'),
(UUID(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', 2, NULL, NOW(), 'ACTIVE'),
(UUID(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000004', 3, NULL, NOW(), 'ACTIVE'),
(UUID(), '00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000005', 4, NULL, NOW(), 'ACTIVE'),
(UUID(), '00000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000006', 5, NULL, NOW(), 'ACTIVE');

-- 4. Danh mục sản phẩm (Nho)
INSERT INTO product_categories (id, name, name_en, category_group, description, is_active, requires_inspection)
VALUES ('00000000-0000-0000-0000-000800000001', 'Nho', 'Grape', 'Cây ăn quả', 'Nho đỏ Ninh Thuận chuẩn VietGAP', TRUE, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), requires_inspection = TRUE, is_active = TRUE;

-- 5. Vùng trồng (GIS có ranh giới GPS Polygon và Point)
INSERT INTO farm_areas (id, organization_id, crop_type, name, area, area_unit, location, boundary, calculated_area, is_active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000100000001',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000800000001',
    'Vùng trồng Nho Ninh Thuận 01',
    3.50,
    'HA',
    ST_GeomFromText('POINT(11.5650 108.9880)'),
    ST_GeomFromText('POLYGON((11.5640 108.9870, 11.5640 108.9890, 11.5660 108.9890, 11.5660 108.9870, 11.5640 108.9870))', 4326),
    3.50,
    TRUE,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE name = VALUES(name), location = VALUES(location), boundary = VALUES(boundary);

-- 6. Lô sản xuất
-- Lô 01: PACKAGED (sẵn sàng tem & tra cứu)
INSERT INTO production_lot (id, organization_id, farm_area_id, product_category_id, name, expected_quantity, expected_quantity_unit, actual_quantity, planting_date, harvest_date, status, approval_notes, created_by, approved_by, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000200000001',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000100000001',
    '00000000-0000-0000-0000-000800000001',
    'Lô Nho 01',
    3000,
    'kg',
    2850,
    '2026-02-15',
    '2026-08-15',
    'PACKAGED',
    'Lô nho đủ điều kiện xuất tem và đạt chứng nhận VietGAP',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000002',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE status = 'PACKAGED', actual_quantity = 2850, name = VALUES(name), approval_notes = VALUES(approval_notes);

-- Lô 02: IN_PROGRESS (Lô tồn đọng > 10 ngày để hiển thị cảnh báo cam trên Kanban)
INSERT INTO production_lot (id, organization_id, farm_area_id, product_category_id, name, expected_quantity, expected_quantity_unit, actual_quantity, planting_date, harvest_date, status, approval_notes, created_by, approved_by, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000200000002',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000100000001',
    '00000000-0000-0000-0000-000800000001',
    'Lô Nho 02 (Cảnh báo tồn đọng)',
    2500,
    'kg',
    NULL,
    '2026-07-01',
    NULL,
    'IN_PROGRESS',
    'Lô tồn đọng cần kiểm tra tiến độ',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000002',
    DATE_SUB(NOW(), INTERVAL 20 DAY),
    DATE_SUB(NOW(), INTERVAL 14 DAY)
)
ON DUPLICATE KEY UPDATE status = 'IN_PROGRESS', name = VALUES(name), approval_notes = VALUES(approval_notes);

-- Lô 03 & Lô 04: Cho bảng Kanban đa dạng
INSERT INTO production_lot (id, organization_id, farm_area_id, product_category_id, name, expected_quantity, expected_quantity_unit, actual_quantity, planting_date, harvest_date, status, approval_notes, created_by, approved_by, created_at, updated_at)
VALUES 
('00000000-0000-0000-0000-000200000003', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000100000001', '00000000-0000-0000-0000-000800000001', 'Lô Nho 03', 2000, 'kg', NULL, '2026-08-01', NULL, 'APPROVED', 'Đã duyệt chuẩn bị canh tác', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', NOW(), NOW()),
('00000000-0000-0000-0000-000200000004', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000100000001', '00000000-0000-0000-0000-000800000001', 'Lô Nho 04', 3200, 'kg', 3100, '2026-03-01', '2026-08-20', 'HARVESTED', 'Đã thu hoạch chờ đóng gói', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), name = VALUES(name);

-- 7. Nhật ký canh tác (Farm logs) cho Lô Nho 01
DELETE FROM farm_logs WHERE production_lot_id = '00000000-0000-0000-0000-000200000001';
INSERT INTO farm_logs (id, production_lot_id, activity_type, material, quantity, unit, executed_date, notes, created_by, created_at)
VALUES
(UUID(), '00000000-0000-0000-0000-000200000001', 'PLANTING', 'Giống nho NH01-48 sạch bệnh', 1200, 'Cây giống', '2026-02-15', 'Xuống giống đúng mật độ kỹ thuật', '00000000-0000-0000-0000-000000000004', NOW()),
(UUID(), '00000000-0000-0000-0000-000200000001', 'FERTILIZING', 'Phân hữu cơ vi sinh Quế Lâm', 500, 'kg', '2026-03-20', 'Bón lót đợt 1 kết hợp làm cỏ quanh gốc', '00000000-0000-0000-0000-000000000004', NOW()),
(UUID(), '00000000-0000-0000-0000-000200000001', 'WATERING', 'Hệ thống tưới nhỏ giọt Israel', 1500, 'Lít', '2026-04-15', 'Tưới nước bổ sung độ ẩm 70%', '00000000-0000-0000-0000-000000000004', NOW()),
(UUID(), '00000000-0000-0000-0000-000200000001', 'FERTILIZING', 'Phân NPK sinh học bón thúc đợt 2', 200, 'kg', '2026-06-10', 'Bón thúc giai đoạn ra hoa đậu trái', '00000000-0000-0000-0000-000000000004', NOW()),
(UUID(), '00000000-0000-0000-0000-000200000001', 'HARVESTING', 'Kéo thu hoạch chuyên dụng', 2850, 'kg', '2026-08-15', 'Thu hoạch vào sáng sớm, độ đường Brix > 16', '00000000-0000-0000-0000-000000000004', NOW());

-- 8. Chứng nhận VietGAP
INSERT INTO certifications (id, organization_id, standard_id, name, issuing_body, code, issued_by, issue_date, expiry_date, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000300000001',
    '00000000-0000-0000-0000-000000000001',
    s.id,
    'Chứng nhận VietGAP - Lô Nho 01',
    'Tổng cục Tiêu chuẩn Đo lường Chất lượng',
    'CERT-VIETGAP-NHO-001',
    'Trung tâm Chứng nhận Phù hợp (QUACERT)',
    '2025-08-01',
    '2027-08-01',
    NOW(),
    NOW()
FROM standards s WHERE s.name = 'VietGAP' LIMIT 1
ON DUPLICATE KEY UPDATE name = VALUES(name), expiry_date = '2027-08-01';

-- Gắn chứng nhận vào Lô Nho 01
DELETE FROM production_lot_certifications WHERE production_lot_id = '00000000-0000-0000-0000-000200000001';
INSERT INTO production_lot_certifications (id, production_lot_id, certification_id, attached_at, attached_by, note)
VALUES (UUID(), '00000000-0000-0000-0000-000200000001', '00000000-0000-0000-0000-000300000001', NOW(), '00000000-0000-0000-0000-000000000002', 'Chứng nhận VietGAP lô nho xuất khẩu');

-- 9. Yêu cầu kiểm nghiệm & Kết quả kiểm nghiệm (PASSED: Chì, Cadmi, E. coli)
INSERT INTO inspection_requests (id, production_lot_id, inspection_unit, sample_sent_date, status, created_by, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000500000001', '00000000-0000-0000-0000-000200000001', 'Trung tâm Kiểm nghiệm Nông Lâm Thủy sản Quốc gia', '2026-08-10', 'PASSED', '00000000-0000-0000-0000-000000000002', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = 'PASSED', inspection_unit = VALUES(inspection_unit);

-- Xóa và nạp lại chỉ tiêu kiểm nghiệm
DELETE FROM inspection_criteria WHERE inspection_request_id = '00000000-0000-0000-0000-000500000001';

INSERT INTO inspection_criteria (id, inspection_request_id, criterion_code, criterion_name, standard_id, criterion_id)
SELECT '00000000-0000-0000-0000-000600000001', '00000000-0000-0000-0000-000500000001', 'HEAVY_METAL_PB', 'Hàm lượng Chì (Pb)', s.id, 6
FROM standards s WHERE s.name = 'VietGAP' LIMIT 1;

INSERT INTO inspection_criteria (id, inspection_request_id, criterion_code, criterion_name, standard_id, criterion_id)
SELECT '00000000-0000-0000-0000-000600000002', '00000000-0000-0000-0000-000500000001', 'HEAVY_METAL_CD', 'Hàm lượng Cadmi (Cd)', s.id, 7
FROM standards s WHERE s.name = 'VietGAP' LIMIT 1;

INSERT INTO inspection_criteria (id, inspection_request_id, criterion_code, criterion_name, standard_id, criterion_id)
SELECT '00000000-0000-0000-0000-000600000003', '00000000-0000-0000-0000-000500000001', 'MICROBIO_E_COLI', 'E. coli', s.id, 12
FROM standards s WHERE s.name = 'VietGAP' LIMIT 1;

-- Kết quả kiểm nghiệm đạt chuẩn (passed = TRUE)
DELETE FROM inspection_criterion_results WHERE inspection_criterion_id IN (
    '00000000-0000-0000-0000-000600000001',
    '00000000-0000-0000-0000-000600000002',
    '00000000-0000-0000-0000-000600000003'
);

INSERT INTO inspection_criterion_results (id, inspection_criterion_id, result_date, expiry_date, passed, file_path, entry_source, created_by, created_at, updated_at)
VALUES
(UUID(), '00000000-0000-0000-0000-000600000001', '2026-08-12', '2027-08-12', TRUE, NULL, 'COOPERATIVE_MANUAL', '00000000-0000-0000-0000-000000000002', NOW(), NOW()),
(UUID(), '00000000-0000-0000-0000-000600000002', '2026-08-12', '2027-08-12', TRUE, NULL, 'COOPERATIVE_MANUAL', '00000000-0000-0000-0000-000000000002', NOW(), NOW()),
(UUID(), '00000000-0000-0000-0000-000600000003', '2026-08-12', '2027-08-12', TRUE, NULL, 'COOPERATIVE_MANUAL', '00000000-0000-0000-0000-000000000002', NOW(), NOW());

-- 10. Dải mã truy xuất & Lô hàng & Tem DEMO-NHO-01
INSERT INTO code_ranges (id, organization_id, prefix, from_number, to_number, total_limit, used_count, created_by, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000c00000001', '00000000-0000-0000-0000-000000000001', 'DEMO', 1, 1000, 1000, 10, '00000000-0000-0000-0000-000000000002', NOW(), NOW())
ON DUPLICATE KEY UPDATE total_limit = 1000;

INSERT INTO shipments (id, production_lot_id, organization_id, code_range_id, name, total_quantity, packaging_info, status, created_by, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000b00000001',
    '00000000-0000-0000-0000-000200000001',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000c00000001',
    'Lô hàng Nho tươi Ninh Thuận VietGAP - Đợt 1',
    50,
    'Thùng carton 5kg (5 hộp x 1kg), màng bao thoáng khí',
    'ACTIVATED',
    '00000000-0000-0000-0000-000000000002',
    '2026-08-16 10:00:00',
    NOW()
)
ON DUPLICATE KEY UPDATE status = 'ACTIVATED', name = VALUES(name);

-- Tem DEMO-NHO-01 (ACTIVE)
INSERT INTO trace_codes (id, shipment_id, code_value, status, activated_at, activated_by, created_at)
VALUES
('00000000-0000-0000-0000-000900000001', '00000000-0000-0000-0000-000b00000001', 'DEMO-NHO-01', 'ACTIVE', '2026-08-17 08:30:00', '00000000-0000-0000-0000-000000000002', '2026-08-16 10:00:00'),
('00000000-0000-0000-0000-000900000002', '00000000-0000-0000-0000-000b00000001', 'DEMO-NHO-02', 'ACTIVE', '2026-08-17 08:30:00', '00000000-0000-0000-0000-000000000002', '2026-08-16 10:00:00'),
('00000000-0000-0000-0000-000900000003', '00000000-0000-0000-0000-000b00000001', 'DEMO-NHO-03', 'ACTIVE', '2026-08-17 08:30:00', '00000000-0000-0000-0000-000000000002', '2026-08-16 10:00:00')
ON DUPLICATE KEY UPDATE status = 'ACTIVE', activated_at = '2026-08-17 08:30:00';

-- Tem có cảnh báo bất thường ANOMALY-DEMO-01
INSERT INTO trace_codes (id, shipment_id, code_value, status, activated_at, activated_by, created_at, suspicion_score, suspicion_reason, high_frequency_score, impossible_travel_score, multiple_locations_score, evaluated_at)
VALUES (
    '00000000-0000-0000-0000-000900000004',
    '00000000-0000-0000-0000-000b00000001',
    'ANOMALY-DEMO-01',
    'ACTIVE',
    '2026-08-17 08:30:00',
    '00000000-0000-0000-0000-000000000002',
    '2026-08-16 10:00:00',
    95,
    'Quét bất thường: Di chuyển bất khả thi giữa Cần Thơ và Hà Nội trong 15 phút (vận tốc ~6000 km/h)',
    40,
    95,
    80,
    NOW()
)
ON DUPLICATE KEY UPDATE suspicion_score = 95, suspicion_reason = VALUES(suspicion_reason);

-- 11. Chuỗi sự kiện Chain Events với mã băm SHA-256 chính xác
DELETE FROM chain_events WHERE shipment_id = '00000000-0000-0000-0000-000b00000001';

INSERT INTO chain_events (id, shipment_id, event_type, event_data, location, recorded_at, recorded_by, created_at, is_correction, hash, previous_hash)
VALUES ('00000000-0000-0000-0000-000a00000001', '00000000-0000-0000-0000-000b00000001', 'HARVEST', '{"activity":"Thu hoạch nho tươi VietGAP","quantity":2850,"unit":"kg"}', ST_GeomFromText('POINT(11.5650 108.9880)'), '2026-08-15 08:00:00', '00000000-0000-0000-0000-000000000002', '2026-08-15 08:05:00', 0, '347635f96c2d7ed1620baa0b0849a9b5b966dacc2edede790b96d196c9cb2a58', NULL);
INSERT INTO chain_events (id, shipment_id, event_type, event_data, location, recorded_at, recorded_by, created_at, is_correction, hash, previous_hash)
VALUES ('00000000-0000-0000-0000-000a00000002', '00000000-0000-0000-0000-000b00000001', 'PREPROCESSING', '{"action":"Làm sạch, phân loại trái và hong khô tự nhiên","temperature":"22°C"}', ST_GeomFromText('POINT(11.5650 108.9880)'), '2026-08-15 14:30:00', '00000000-0000-0000-0000-000000000002', '2026-08-15 14:35:00', 0, 'ba005c203457f3adbf122db8a083e64cbd51882adf84b7b645ec2d14874be67f', '347635f96c2d7ed1620baa0b0849a9b5b966dacc2edede790b96d196c9cb2a58');
INSERT INTO chain_events (id, shipment_id, event_type, event_data, location, recorded_at, recorded_by, created_at, is_correction, hash, previous_hash)
VALUES ('00000000-0000-0000-0000-000a00000003', '00000000-0000-0000-0000-000b00000001', 'PACKAGING', '{"packageType":"Thùng carton 5kg (5 hộp x 1kg)","totalBoxes":50}', ST_GeomFromText('POINT(11.5650 108.9880)'), '2026-08-16 09:00:00', '00000000-0000-0000-0000-000000000002', '2026-08-16 09:05:00', 0, '8ba55ff1d93bb945645d1ddb18ffeba99f646b3d0b08200272892ad00689c754', 'ba005c203457f3adbf122db8a083e64cbd51882adf84b7b645ec2d14874be67f');
INSERT INTO chain_events (id, shipment_id, event_type, event_data, location, recorded_at, recorded_by, created_at, is_correction, hash, previous_hash)
VALUES ('00000000-0000-0000-0000-000a00000004', '00000000-0000-0000-0000-000b00000001', 'WAREHOUSE_ENTRY', '{"storageTemp":"5°C","warehouse":"Tổng kho lạnh HTX Demo"}', ST_GeomFromText('POINT(11.5650 108.9880)'), '2026-08-16 15:00:00', '00000000-0000-0000-0000-000000000002', '2026-08-16 15:05:00', 0, '7799f3a1fe9f539f0f95d0e53444ff87e38e28954b00ec3ecf57947089985455', '8ba55ff1d93bb945645d1ddb18ffeba99f646b3d0b08200272892ad00689c754');

-- 12. Phiếu bàn giao (Shipment Handover) cho Doanh nghiệp thu mua (Tab 3)
DELETE FROM shipment_handovers WHERE shipment_id = '00000000-0000-0000-0000-000b00000001';

INSERT INTO shipment_handovers (
    id, shipment_id, from_organization_id, to_organization_id, quantity, status,
    planned_at, vehicle_info, carrier_name, note, expires_at, created_by, created_at
) VALUES (
    '00000000-0000-0000-0000-000e00000001',
    '00000000-0000-0000-0000-000b00000001',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000010',
    50,
    'PENDING_CONFIRMATION',
    DATE_ADD(NOW(), INTERVAL 1 DAY),
    'Xe tải đông lạnh chuyên dụng 29H-123.45',
    'Tài xế Nguyễn Văn Vận',
    'Bàn giao 50 thùng Nho tươi Ninh Thuận tiêu chuẩn VietGAP xuất khẩu',
    DATE_ADD(NOW(), INTERVAL 7 DAY),
    '00000000-0000-0000-0000-000000000002',
    NOW()
);

-- 13. Nhật ký quét bất thường (Trace code scan logs) cho Tab 4
DELETE FROM trace_code_scan_logs WHERE trace_code_id = '00000000-0000-0000-0000-000900000004';

INSERT INTO trace_code_scan_logs (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal, abnormal_reason)
VALUES
(
    '00000000-0000-0000-0000-000921000101',
    '00000000-0000-0000-0000-000900000004',
    DATE_SUB(NOW(), INTERVAL 45 MINUTE),
    '113.164.20.10',
    'Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X)',
    10.0340000,
    105.7780000,
    'Cần Thơ - Siêu thị GO! Cần Thơ',
    FALSE,
    NULL
),
(
    '00000000-0000-0000-0000-000921000102',
    '00000000-0000-0000-0000-000900000004',
    DATE_SUB(NOW(), INTERVAL 30 MINUTE),
    '171.244.15.80',
    'Mozilla/5.0 (Linux; Android 14; SM-S918B)',
    21.0285000,
    105.8542000,
    'Hà Nội - Siêu thị WinMart Tràng Tiền',
    TRUE,
    'Phát hiện di chuyển bất hợp lý giữa Cần Thơ và Hà Nội trong 15 phút (khoảng cách ~1,500km, vận tốc ~6000 km/h) - Nghi vấn tem bị nhân bản trái phép'
);

-- 14. Yêu cầu thu hồi (Recall Request) phục vụ kịch bản Phân đoạn 6
-- Trạng thái PENDING_APPROVAL / PENDING, tạo bởi orgmanager, chờ orgmanager2 phê duyệt
DELETE FROM recall_requests WHERE shipment_id = '00000000-0000-0000-0000-000b00000001';
INSERT INTO recall_requests (
    id, production_lot_id, shipment_id, requested_by, requested_at, reason, evidence, status, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000d00000001',
    '00000000-0000-0000-0000-000200000001',
    '00000000-0000-0000-0000-000b00000001',
    '00000000-0000-0000-0000-000000000002',
    NOW(),
    'CẢNH BÁO: Lô hàng đang bị thu hồi do phát hiện mẫu ngẫu nhiên ngoài thị trường có nguy cơ nhiễm khuẩn. Khuyến cáo không sử dụng và liên hệ hotline HTX: 0912.000.031!',
    'Biên bản kiểm tra mẫu số 24/BB-SYT của Sở Y tế ngày 28/09/2026',
    'PENDING',
    NOW(),
    NOW()
);

-- Yêu cầu thu hồi hàng loạt (Bulk recall request)
DELETE FROM bulk_recall_shipments WHERE shipment_id = '00000000-0000-0000-0000-000b00000001';
DELETE FROM bulk_recall_requests WHERE production_lot_id = '00000000-0000-0000-0000-000200000001';

INSERT INTO bulk_recall_requests (
    id, production_lot_id, reason, evidence, status, requested_by, requested_at, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000d00000002',
    '00000000-0000-0000-0000-000200000001',
    'CẢNH BÁO: Lô hàng đang bị thu hồi do phát hiện mẫu ngẫu nhiên ngoài thị trường có nguy cơ nhiễm khuẩn. Khuyến cáo không sử dụng và liên hệ hotline HTX: 0912.000.031!',
    'Biên bản kiểm tra mẫu số 24/BB-SYT của Sở Y tế ngày 28/09/2026',
    'PENDING',
    '00000000-0000-0000-0000-000000000002',
    NOW(),
    NOW(),
    NOW()
);

INSERT INTO bulk_recall_shipments (id, bulk_recall_request_id, shipment_id, included, created_at)
VALUES (UUID(), '00000000-0000-0000-0000-000d00000002', '00000000-0000-0000-0000-000b00000001', 1, NOW());

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'SEED DEMO DATA COMPLETED SUCCESSFULLY!' AS result;
