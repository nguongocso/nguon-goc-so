-- ============================================================
-- V20260914110500: Seed ProfileTemplate test data (NCL-07-CN-007)
-- 1 org có 2 template (1 default đầy đủ, 1 custom 10 trường)
-- 1 template thuộc org KHÁC (test TC-04)
-- ============================================================

-- 1. Đảm bảo tồn tại Org khác (Org B)
INSERT IGNORE INTO organizations (
    organization_id, name, code, type, status, address, phone, email, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000100000002',
    'HTX Nông Nghiệp Khác',
    'HTX_OTHER',
    'COOPERATIVE',
    'ACTIVE',
    'Tỉnh Đồng Tháp',
    '0909999999',
    'htx.khac@test.nguongocso.vn',
    NOW(),
    NOW()
);

-- 2. Template 1 (Org A - Default đầy đủ)
INSERT IGNORE INTO profile_templates (
    id, organization_id, name, partner_name, description, is_default, created_at, updated_at, created_by
) VALUES (
    '00000000-0000-0000-0000-000e00000001',
    '00000000-0000-0000-0000-000100000001',
    'Mẫu mặc định đầy đủ',
    'Áp dụng chung',
    'Mẫu hồ sơ mặc định đầy đủ trường thông tin',
    TRUE,
    NOW(),
    NOW(),
    '00000000-0000-0000-0000-000200000001'
);

-- Các trường cho Template 1 (14 trường)
INSERT IGNORE INTO profile_template_fields (id, template_id, field_key, field_group, is_mandatory, sort_order) VALUES
('00000000-0000-0000-0000-000f00000001', '00000000-0000-0000-0000-000e00000001', 'organization.name', 'ORGANIZATION', TRUE, 1),
('00000000-0000-0000-0000-000f00000002', '00000000-0000-0000-0000-000e00000001', 'organization.address', 'ORGANIZATION', FALSE, 2),
('00000000-0000-0000-0000-000f00000003', '00000000-0000-0000-0000-000e00000001', 'farmArea.name', 'FARM_AREA', TRUE, 3),
('00000000-0000-0000-0000-000f00000004', '00000000-0000-0000-0000-000e00000001', 'farmArea.location', 'FARM_AREA', FALSE, 4),
('00000000-0000-0000-0000-000f00000005', '00000000-0000-0000-0000-000e00000001', 'productionLot.name', 'PRODUCTION_LOT', TRUE, 5),
('00000000-0000-0000-0000-000f00000006', '00000000-0000-0000-0000-000e00000001', 'productionLot.productCategory', 'PRODUCTION_LOT', TRUE, 6),
('00000000-0000-0000-0000-000f00000007', '00000000-0000-0000-0000-000e00000001', 'productionLot.harvestDate', 'PRODUCTION_LOT', FALSE, 7),
('00000000-0000-0000-0000-000f00000008', '00000000-0000-0000-0000-000e00000001', 'shipment.name', 'SHIPMENT', TRUE, 8),
('00000000-0000-0000-0000-000f00000009', '00000000-0000-0000-0000-000e00000001', 'shipment.totalQuantity', 'SHIPMENT', TRUE, 9),
('00000000-0000-0000-0000-000f00000010', '00000000-0000-0000-0000-000e00000001', 'shipment.packagingInfo', 'SHIPMENT', FALSE, 10),
('00000000-0000-0000-0000-000f00000011', '00000000-0000-0000-0000-000e00000001', 'farmLog.activityType', 'FARM_LOG', TRUE, 11),
('00000000-0000-0000-0000-000f00000012', '00000000-0000-0000-0000-000e00000001', 'farmLog.material', 'FARM_LOG', FALSE, 12),
('00000000-0000-0000-0000-000f00000013', '00000000-0000-0000-0000-000e00000001', 'inspection.passed', 'INSPECTION', FALSE, 13),
('00000000-0000-0000-0000-000f00000014', '00000000-0000-0000-0000-000e00000001', 'chainEvent.eventType', 'CHAIN_EVENT', TRUE, 14);

-- 3. Template 2 (Org A - Custom 10 trường, đối tác Co.opmart)
INSERT IGNORE INTO profile_templates (
    id, organization_id, name, partner_name, description, is_default, created_at, updated_at, created_by
) VALUES (
    '00000000-0000-0000-0000-000e00000002',
    '00000000-0000-0000-0000-000100000001',
    'Mẫu Co.opmart 10 trường',
    'Saigon Co.op',
    'Biểu mẫu tinh gọn 10 trường dành cho chuỗi siêu thị Co.opmart',
    FALSE,
    NOW(),
    NOW(),
    '00000000-0000-0000-0000-000200000001'
);

-- Đúng 10 trường (8 trường bắt buộc QTN-11 + 2 trường tùy chọn: productionLot.harvestDate, inspection.passed)
INSERT IGNORE INTO profile_template_fields (id, template_id, field_key, field_group, is_mandatory, sort_order) VALUES
('00000000-0000-0000-0000-000f00000021', '00000000-0000-0000-0000-000e00000002', 'organization.name', 'ORGANIZATION', TRUE, 1),
('00000000-0000-0000-0000-000f00000022', '00000000-0000-0000-0000-000e00000002', 'farmArea.name', 'FARM_AREA', TRUE, 2),
('00000000-0000-0000-0000-000f00000023', '00000000-0000-0000-0000-000e00000002', 'productionLot.name', 'PRODUCTION_LOT', TRUE, 3),
('00000000-0000-0000-0000-000f00000024', '00000000-0000-0000-0000-000e00000002', 'productionLot.productCategory', 'PRODUCTION_LOT', TRUE, 4),
('00000000-0000-0000-0000-000f00000025', '00000000-0000-0000-0000-000e00000002', 'productionLot.harvestDate', 'PRODUCTION_LOT', FALSE, 5),
('00000000-0000-0000-0000-000f00000026', '00000000-0000-0000-0000-000e00000002', 'shipment.name', 'SHIPMENT', TRUE, 6),
('00000000-0000-0000-0000-000f00000027', '00000000-0000-0000-0000-000e00000002', 'shipment.totalQuantity', 'SHIPMENT', TRUE, 7),
('00000000-0000-0000-0000-000f00000028', '00000000-0000-0000-0000-000e00000002', 'farmLog.activityType', 'FARM_LOG', TRUE, 8),
('00000000-0000-0000-0000-000f00000029', '00000000-0000-0000-0000-000e00000002', 'inspection.passed', 'INSPECTION', FALSE, 9),
('00000000-0000-0000-0000-000f00000030', '00000000-0000-0000-0000-000e00000002', 'chainEvent.eventType', 'CHAIN_EVENT', TRUE, 10);

-- 4. Template 3 (Org B - HTX Khác, dùng để test TC-04 cách ly dữ liệu)
INSERT IGNORE INTO profile_templates (
    id, organization_id, name, partner_name, description, is_default, created_at, updated_at, created_by
) VALUES (
    '00000000-0000-0000-0000-000e00000003',
    '00000000-0000-0000-0000-000100000002',
    'Mẫu nội bộ HTX Khác',
    'Nội bộ',
    'Mẫu của HTX khác, HTX_TEST không được nhìn thấy',
    TRUE,
    NOW(),
    NOW(),
    NULL
);

INSERT IGNORE INTO profile_template_fields (id, template_id, field_key, field_group, is_mandatory, sort_order) VALUES
('00000000-0000-0000-0000-000f00000041', '00000000-0000-0000-0000-000e00000003', 'organization.name', 'ORGANIZATION', TRUE, 1),
('00000000-0000-0000-0000-000f00000042', '00000000-0000-0000-0000-000e00000003', 'farmArea.name', 'FARM_AREA', TRUE, 2),
('00000000-0000-0000-0000-000f00000043', '00000000-0000-0000-0000-000e00000003', 'productionLot.name', 'PRODUCTION_LOT', TRUE, 3),
('00000000-0000-0000-0000-000f00000044', '00000000-0000-0000-0000-000e00000003', 'productionLot.productCategory', 'PRODUCTION_LOT', TRUE, 4),
('00000000-0000-0000-0000-000f00000045', '00000000-0000-0000-0000-000e00000003', 'shipment.name', 'SHIPMENT', TRUE, 5),
('00000000-0000-0000-0000-000f00000046', '00000000-0000-0000-0000-000e00000003', 'shipment.totalQuantity', 'SHIPMENT', TRUE, 6),
('00000000-0000-0000-0000-000f00000047', '00000000-0000-0000-0000-000e00000003', 'farmLog.activityType', 'FARM_LOG', TRUE, 7),
('00000000-0000-0000-0000-000f00000048', '00000000-0000-0000-0000-000e00000003', 'chainEvent.eventType', 'CHAIN_EVENT', TRUE, 8);
