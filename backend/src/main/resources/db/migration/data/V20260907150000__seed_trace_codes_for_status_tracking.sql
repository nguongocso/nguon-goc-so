-- ============================================================
-- V20260907150000: Seed demo shipment, trace codes and scan logs
-- for US NCL-04-CN-008: View and trace individual code status within a shipment
-- ============================================================

-- 1. Create a demo shipment if a production_lot exists
INSERT IGNORE INTO shipments (
    id,
    production_lot_id,
    organization_id,
    name,
    total_quantity,
    packaging_info,
    status,
    created_by,
    created_at,
    updated_at
)
SELECT
    '99999999-0000-0000-0000-000000000001',
    p.id,
    p.organization_id,
    'Lô hàng bưởi Phúc Trạch - Đợt xuất khẩu mẫu',
    25,
    'Thùng carton 10kg',
    'ACTIVATED',
    (SELECT user_id FROM users LIMIT 1),
    NOW(),
    NOW()
FROM production_lot p
LIMIT 1;

-- 2. Trace codes across all statuses (INACTIVE, ACTIVE, LOCKED, CANCELLED, RECALLED)
-- INACTIVE (5 codes)
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at)
SELECT '99999999-0000-0000-0001-000000000001', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-INACT-001', 'INACTIVE', NOW(), NULL FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at)
SELECT '99999999-0000-0000-0001-000000000002', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-INACT-002', 'INACTIVE', NOW(), NULL FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at)
SELECT '99999999-0000-0000-0001-000000000003', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-INACT-003', 'INACTIVE', NOW(), NOW() FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at)
SELECT '99999999-0000-0000-0001-000000000004', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-INACT-004', 'INACTIVE', NOW(), NOW() FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at)
SELECT '99999999-0000-0000-0001-000000000005', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-INACT-005', 'INACTIVE', NOW(), NULL FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';

-- ACTIVE (8 codes)
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, activated_by)
SELECT '99999999-0000-0000-0002-000000000001', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-ACT-001', 'ACTIVE', NOW(), NOW(), NOW(), (SELECT user_id FROM users LIMIT 1) FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, activated_by)
SELECT '99999999-0000-0000-0002-000000000002', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-ACT-002', 'ACTIVE', NOW(), NOW(), NOW(), (SELECT user_id FROM users LIMIT 1) FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, activated_by)
SELECT '99999999-0000-0000-0002-000000000003', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-ACT-003', 'ACTIVE', NOW(), NOW(), NOW(), (SELECT user_id FROM users LIMIT 1) FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, activated_by)
SELECT '99999999-0000-0000-0002-000000000004', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-ACT-004', 'ACTIVE', NOW(), NOW(), NOW(), (SELECT user_id FROM users LIMIT 1) FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, activated_by)
SELECT '99999999-0000-0000-0002-000000000005', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-ACT-005', 'ACTIVE', NOW(), NOW(), NOW(), (SELECT user_id FROM users LIMIT 1) FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, activated_by)
SELECT '99999999-0000-0000-0002-000000000006', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-ACT-006', 'ACTIVE', NOW(), NOW(), NOW(), (SELECT user_id FROM users LIMIT 1) FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, activated_by)
SELECT '99999999-0000-0000-0002-000000000007', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-ACT-007', 'ACTIVE', NOW(), NOW(), NOW(), (SELECT user_id FROM users LIMIT 1) FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, activated_by)
SELECT '99999999-0000-0000-0002-000000000008', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-ACT-008', 'ACTIVE', NOW(), NOW(), NOW(), (SELECT user_id FROM users LIMIT 1) FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';

-- LOCKED (4 codes)
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, locked_at, lock_reason)
SELECT '99999999-0000-0000-0003-000000000001', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-LOCK-001', 'LOCKED', NOW(), NOW(), NOW(), NOW(), 'Nghi vấn quét bất thường từ 2 địa bàn' FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, locked_at, lock_reason)
SELECT '99999999-0000-0000-0003-000000000002', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-LOCK-002', 'LOCKED', NOW(), NOW(), NOW(), NOW(), 'Tần suất quét vượt ngưỡng cho phép trong 5 phút' FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, locked_at, lock_reason)
SELECT '99999999-0000-0000-0003-000000000003', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-LOCK-003', 'LOCKED', NOW(), NOW(), NOW(), NOW(), 'Báo cáo nghi vấn từ người tiêu dùng' FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at, locked_at, lock_reason)
SELECT '99999999-0000-0000-0003-000000000004', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-LOCK-004', 'LOCKED', NOW(), NOW(), NOW(), NOW(), 'Tạm khóa để kiểm tra tem thực địa' FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';

-- CANCELLED (4 codes)
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, cancelled_at, cancel_reason_type, cancel_reason)
SELECT '99999999-0000-0000-0004-000000000001', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-CANC-001', 'CANCELLED', NOW(), NOW(), NOW(), 'PRINT_ERROR', 'Tem in bị nhòe mực không thể quét' FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, cancelled_at, cancel_reason_type, cancel_reason)
SELECT '99999999-0000-0000-0004-000000000002', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-CANC-002', 'CANCELLED', NOW(), NOW(), NOW(), 'PRINT_MISALIGNED', 'In lệch lề cắt góc QR' FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, cancelled_at, cancel_reason_type, cancel_reason)
SELECT '99999999-0000-0000-0004-000000000003', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-CANC-003', 'CANCELLED', NOW(), NOW(), NOW(), 'PEELED_OFF_DAMAGED', 'Rách tem khi dán vào thùng' FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, cancelled_at, cancel_reason_type, cancel_reason)
SELECT '99999999-0000-0000-0004-000000000004', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-CANC-004', 'CANCELLED', NOW(), NOW(), NOW(), 'OTHER', 'Hủy do đổi quy cách đóng gói' FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';

-- RECALLED (4 codes)
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at)
SELECT '99999999-0000-0000-0005-000000000001', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-RECL-001', 'RECALLED', NOW(), NOW(), NOW() FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at)
SELECT '99999999-0000-0000-0005-000000000002', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-RECL-002', 'RECALLED', NOW(), NOW(), NOW() FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at)
SELECT '99999999-0000-0000-0005-000000000003', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-RECL-003', 'RECALLED', NOW(), NOW(), NOW() FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';
INSERT IGNORE INTO trace_codes (id, shipment_id, code_value, status, created_at, printed_at, activated_at)
SELECT '99999999-0000-0000-0005-000000000004', '99999999-0000-0000-0000-000000000001', 'DEMO-NCL04-RECL-004', 'RECALLED', NOW(), NOW(), NOW() FROM shipments WHERE id = '99999999-0000-0000-0000-000000000001';

-- 3. Scan logs for ACTIVE code 'DEMO-NCL04-ACT-001' (4 scans)
INSERT IGNORE INTO trace_code_scan_logs (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal)
SELECT '99999999-0000-0001-0001-000000000001', '99999999-0000-0000-0002-000000000001', DATE_SUB(NOW(), INTERVAL 3 HOUR), '14.162.180.10', 'Mozilla/5.0 (iPhone)', 21.0285, 105.8542, 'Hà Nội', FALSE FROM trace_codes WHERE id = '99999999-0000-0000-0002-000000000001';
INSERT IGNORE INTO trace_code_scan_logs (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal)
SELECT '99999999-0000-0001-0001-000000000002', '99999999-0000-0000-0002-000000000001', DATE_SUB(NOW(), INTERVAL 2 HOUR), '14.162.180.12', 'Mozilla/5.0 (Android)', 21.0285, 105.8542, 'Hà Nội', FALSE FROM trace_codes WHERE id = '99999999-0000-0000-0002-000000000001';
INSERT IGNORE INTO trace_code_scan_logs (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal)
SELECT '99999999-0000-0001-0001-000000000003', '99999999-0000-0000-0002-000000000001', DATE_SUB(NOW(), INTERVAL 1 HOUR), '113.161.72.50', 'Mozilla/5.0 (Windows)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', FALSE FROM trace_codes WHERE id = '99999999-0000-0000-0002-000000000001';
INSERT IGNORE INTO trace_code_scan_logs (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal)
SELECT '99999999-0000-0001-0001-000000000004', '99999999-0000-0000-0002-000000000001', NOW(), '113.161.72.55', 'Mozilla/5.0 (iPhone)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', FALSE FROM trace_codes WHERE id = '99999999-0000-0000-0002-000000000001';
