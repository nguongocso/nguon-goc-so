-- ============================================================
-- V61: Seed dữ liệu kiểm thử phát hiện quét bất thường (NCL-08-CN-014)
--
-- Tạo 1 lô hàng + 3 mã tem ACTIVE + 56 lượt quét trải dài 30 ngày trong
-- bảng trace_code_scan_logs để phục vụ cấu hình & ước lượng ngưỡng bất thường:
--   - ANOMALY-TEST-001: 14 lượt quét bình thường tại Hà Nội (mỗi 2-3 ngày).
--   - ANOMALY-TEST-002: lượt quét tần suất cao trong 1 giờ + di chuyển bất hợp lý
--     liên tỉnh (Hà Nội -> Đà Nẵng -> TP.HCM, mỗi chặng ~20 phút).
--   - ANOMALY-TEST-003: 5 địa điểm khác nhau trong ~1.5 giờ + 12 lượt trong 66 phút.
--
-- Phụ thuộc dữ liệu seed V58: org DEMO_HTX, user 'orgmanager', lô sản xuất
-- '00000000-0000-0000-0000-000200000001' (Nho, trạng thái PACKAGED).
-- Idempotent: tất cả ID cố định + INSERT IGNORE.
-- ============================================================

-- 1. Lô hàng demo phục vụ kiểm thử ngưỡng bất thường
INSERT IGNORE INTO shipments
    (id, production_lot_id, organization_id, name, total_quantity, packaging_info,
     status, created_by, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000900000001',
    '00000000-0000-0000-0000-000200000001',
    (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX'),
    'Lô hàng kiểm thử ngưỡng bất thường (NCL-08-CN-014)',
    3,
    'Hộp 3 tem kiểm thử',
    'ACTIVATED',
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    NOW(),
    NOW();

-- 2. Ba mã tem ACTIVE đã kích hoạt (25-28 ngày trước)
INSERT IGNORE INTO trace_codes
    (id, shipment_id, code_value, qr_image, status, activated_at, activated_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000002',
    '00000000-0000-0000-0000-000900000001',
    'ANOMALY-TEST-001', NULL, 'ACTIVE',
    DATE_SUB(NOW(), INTERVAL 28 DAY),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    DATE_SUB(NOW(), INTERVAL 28 DAY);

INSERT IGNORE INTO trace_codes
    (id, shipment_id, code_value, qr_image, status, activated_at, activated_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000003',
    '00000000-0000-0000-0000-000900000001',
    'ANOMALY-TEST-002', NULL, 'ACTIVE',
    DATE_SUB(NOW(), INTERVAL 25 DAY),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    DATE_SUB(NOW(), INTERVAL 25 DAY);

INSERT IGNORE INTO trace_codes
    (id, shipment_id, code_value, qr_image, status, activated_at, activated_by, created_at)
SELECT
    '00000000-0000-0000-0000-000900000004',
    '00000000-0000-0000-0000-000900000001',
    'ANOMALY-TEST-003', NULL, 'ACTIVE',
    DATE_SUB(NOW(), INTERVAL 26 DAY),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager'),
    DATE_SUB(NOW(), INTERVAL 26 DAY);

-- 3. ANOMALY-TEST-001: 14 lượt quét bình thường tại Hà Nội (mỗi 2 ngày)
INSERT IGNORE INTO trace_code_scan_logs
    (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal, abnormal_reason)
SELECT
    CONCAT('00000000-0000-0000-0000-000910', LPAD(t.i, 6, '0')),
    '00000000-0000-0000-0000-000900000002',
    DATE_SUB(NOW(), INTERVAL (28 - t.i * 2) DAY),
    CONCAT('113.164.', 10 + t.i, '.', 20 + t.i),
    'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15',
    21.0285000 + t.i * 0.0001000,
    105.8542000 + t.i * 0.0001000,
    CONCAT('Hà Nội - điểm quét định kỳ ', t.i),
    FALSE, NULL
FROM (
    SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
    UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11
    UNION ALL SELECT 12 UNION ALL SELECT 13
) t;

-- 4. ANOMALY-TEST-002: 6 lượt quét trong 25 phút (vượt maxScansPerHour = 5)
INSERT IGNORE INTO trace_code_scan_logs
    (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal, abnormal_reason)
SELECT
    CONCAT('00000000-0000-0000-0000-000920', LPAD(t.i, 6, '0')),
    '00000000-0000-0000-0000-000900000003',
    DATE_SUB(NOW(), INTERVAL (480 - t.i * 5) MINUTE),
    CONCAT('14.224.', 30 + t.i, '.', 40 + t.i),
    'Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36',
    21.0285000, 105.8542000,
    'Hà Nội - quét dồn dập',
    FALSE, NULL
FROM (
    SELECT 1 i UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) t;

-- 5. ANOMALY-TEST-002: di chuyển bất hợp lý Hà Nội -> Đà Nẵng -> TP.HCM
INSERT IGNORE INTO trace_code_scan_logs
    (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal, abnormal_reason)
VALUES
('00000000-0000-0000-0000-0009210001', '00000000-0000-0000-0000-000900000003',
 DATE_SUB(NOW(), INTERVAL 40 MINUTE), '113.164.99.11',
 'Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36',
 21.0285000, 105.8542000, 'Hà Nội - điểm xuất phát', FALSE, NULL),
('00000000-0000-0000-0000-0009210002', '00000000-0000-0000-0000-000900000003',
 DATE_SUB(NOW(), INTERVAL 20 MINUTE), '118.70.15.201',
 'Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36',
 16.0544000, 108.2022000, 'Đà Nẵng - di chuyển bất hợp lý', FALSE, NULL),
('00000000-0000-0000-0000-0009210003', '00000000-0000-0000-0000-000900000003',
 NOW(), '171.244.23.98',
 'Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36',
 10.7769000, 106.7009000, 'TP. Hồ Chí Minh - di chuyển bất hợp lý', FALSE, NULL);

-- 6. ANOMALY-TEST-002: cặp di chuyển bất hợp lý bổ sung (15 ngày trước)
INSERT IGNORE INTO trace_code_scan_logs
    (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal, abnormal_reason)
VALUES
('00000000-0000-0000-0000-0009210004', '00000000-0000-0000-0000-000900000003',
 DATE_SUB(NOW(), INTERVAL 15 DAY), '113.164.99.12',
 'Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36',
 21.0285000, 105.8542000, 'Hà Nội', FALSE, NULL),
('00000000-0000-0000-0000-0009210005', '00000000-0000-0000-0000-000900000003',
 DATE_ADD(DATE_SUB(NOW(), INTERVAL 15 DAY), INTERVAL 10 MINUTE), '171.244.23.99',
 'Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36',
 10.7769000, 106.7009000, 'TP. Hồ Chí Minh', FALSE, NULL);

-- 7. ANOMALY-TEST-003: 5 địa điểm khác nhau trong ~1.5 giờ (10 ngày trước)
INSERT IGNORE INTO trace_code_scan_logs
    (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal, abnormal_reason)
VALUES
('00000000-0000-0000-0000-0009300001', '00000000-0000-0000-0000-000900000004',
 DATE_SUB(NOW(), INTERVAL 10 DAY), '14.185.10.21',
 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15',
 21.0285000, 105.8542000, 'Hà Nội - Hoàn Kiếm', FALSE, NULL),
('00000000-0000-0000-0000-0009300002', '00000000-0000-0000-0000-000900000004',
 DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 20 MINUTE), '14.185.10.22',
 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15',
 20.9930000, 105.8470000, 'Hà Nội - Hoàng Mai', FALSE, NULL),
('00000000-0000-0000-0000-0009300003', '00000000-0000-0000-0000-000900000004',
 DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 40 MINUTE), '14.185.10.23',
 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15',
 21.0500000, 105.7800000, 'Hà Nội - Bắc Từ Liêm', FALSE, NULL),
('00000000-0000-0000-0000-0009300004', '00000000-0000-0000-0000-000900000004',
 DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 60 MINUTE), '14.185.10.24',
 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15',
 20.9700000, 105.7900000, 'Hà Nội - Hà Đông', FALSE, NULL),
('00000000-0000-0000-0000-0009300005', '00000000-0000-0000-0000-000900000004',
 DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 80 MINUTE), '14.185.10.25',
 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15',
 21.1000000, 105.9200000, 'Bắc Ninh - Từ Sơn', FALSE, NULL);

-- 8. ANOMALY-TEST-003: 12 lượt quét trong 66 phút (vượt maxScansPerDay = 10)
INSERT IGNORE INTO trace_code_scan_logs
    (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal, abnormal_reason)
SELECT
    CONCAT('00000000-0000-0000-0000-000931', LPAD(t.i, 6, '0')),
    '00000000-0000-0000-0000-000900000004',
    DATE_SUB(NOW(), INTERVAL (700 - t.i * 6) MINUTE),
    CONCAT('14.185.', 40 + t.i, '.', 50 + t.i),
    'Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15',
    21.0285000 + t.i * 0.0002000,
    105.8542000 + t.i * 0.0002000,
    'Hà Nội - quét tần suất cao',
    FALSE, NULL
FROM (
    SELECT 1 i UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
    UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
) t;

-- 9. ANOMALY-TEST-003: các lượt quét rải rác khác trong 30 ngày
INSERT IGNORE INTO trace_code_scan_logs
    (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal, abnormal_reason)
SELECT
    CONCAT('00000000-0000-0000-0000-000932', LPAD(t.i, 6, '0')),
    '00000000-0000-0000-0000-000900000004',
    DATE_SUB(NOW(), INTERVAL (25 - t.i * 3) DAY),
    CONCAT('113.164.', 60 + t.i, '.', 70 + t.i),
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    21.0285000, 105.8542000,
    CONCAT('Hà Nội - lượt quét định kỳ ', t.i),
    FALSE, NULL
FROM (
    SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
) t;

-- 10. Lượt quét không có tọa độ (kiểm thử nhánh xử lý NULL)
INSERT IGNORE INTO trace_code_scan_logs
    (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal, abnormal_reason)
VALUES
('00000000-0000-0000-0000-0009400001', '00000000-0000-0000-0000-000900000002',
 DATE_SUB(NOW(), INTERVAL 13 DAY), '113.164.88.5',
 'Mozilla/5.0 (compatible; QR-Scanner/2.1)', NULL, NULL, 'Không xác định (không cấp quyền vị trí)', FALSE, NULL),
('00000000-0000-0000-0000-0009400002', '00000000-0000-0000-0000-000900000003',
 DATE_SUB(NOW(), INTERVAL 6 DAY), '14.224.77.8',
 'Mozilla/5.0 (compatible; QR-Scanner/2.1)', NULL, NULL, 'Không xác định (không cấp quyền vị trí)', FALSE, NULL);
