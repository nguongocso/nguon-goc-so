-- ============================================================
-- V20260831100000: Seed dữ liệu kiểm thử phát hiện quét bất thường cho Loại Lúa (3 lượt / giờ)
--
-- Kịch bản kiểm thử:
--   - Loại nông sản: "Lúa" (ngưỡng maxScansPerHour = 2).
--   - Mã tem: NCL-TEST-LUA-001 (thuộc lô hàng lúa ST25).
--   - 3 lượt quét trong vòng 40 phút (T-40m, T-20m, NOW) tại Hà Nội, Đà Nẵng, TP.HCM.
--   - Kết quả đánh giá:
--       + Tần suất cao: 3 lượt / giờ >= maxPerHour (2) -> +30 điểm
--       + Di chuyển bất hợp lý: Hà Nội -> Đà Nẵng -> TP.HCM (625km / 20m) -> +40 điểm
--       + Tổng điểm nghi vấn = 70 (>= 50) -> Trạng thái SUSPECT.
-- ============================================================

-- 1. Đảm bảo Loại nông sản "Lúa" tồn tại
INSERT IGNORE INTO product_categories (id, name, category_group, description, is_active)
SELECT
    '00000000-0000-0000-0000-000800000004',
    'Lúa',
    'Lương thực',
    'Lúa gạo kiểm thử phát hiện quét bất thường',
    TRUE
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM product_categories WHERE id = '00000000-0000-0000-0000-000800000004'
);

-- 2. Đảm bảo Ngưỡng quét bất thường cho Loại nông sản "Lúa" là 2 lượt / giờ
INSERT INTO anomaly_thresholds
    (id, product_category_id, max_scans_per_hour, max_scans_per_day,
     max_distance_km_per_30min, min_time_between_scans_minutes, activation_age_days,
     is_active, created_by, updated_by, created_at, updated_at)
SELECT
    'daa9db6f-ed69-4ddc-b122-ea644cb10b63',
    '00000000-0000-0000-0000-000800000004',
    2, 30, 50.00, 30, 365, TRUE,
    (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1),
    (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1),
    NOW(), NOW()
FROM DUAL
ON DUPLICATE KEY UPDATE
    max_scans_per_hour = 2,
    max_scans_per_day = 30,
    is_active = TRUE,
    updated_by = (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1),
    updated_at = NOW();

-- 3. Tạo lô hàng cho Lô Lúa (liên kết với production_lot Lúa có sẵn '00000000-0000-0000-0000-000200000004')
INSERT IGNORE INTO shipments
    (id, production_lot_id, organization_id, name, total_quantity, packaging_info,
     status, created_by, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000900000010',
    '00000000-0000-0000-0000-000200000004',
    (SELECT organization_id FROM organizations WHERE code = 'DEMO_HTX' LIMIT 1),
    'Lô hàng Lúa ST25 Kiểm thử ngưỡng 3 lần quét / giờ (NCL-08-CN-014)',
    1,
    'Bao 25kg có dán tem QR',
    'ACTIVATED',
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    NOW(),
    NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM shipments WHERE id = '00000000-0000-0000-0000-000900000010'
);

-- 4. Tạo mã tem cho Lô hàng Lúa ST25
INSERT INTO trace_codes
    (id, shipment_id, code_value, qr_image, status, activated_at, activated_by, created_at,
     suspicion_score, suspicion_reason)
SELECT
    '00000000-0000-0000-0000-000900000020',
    '00000000-0000-0000-0000-000900000010',
    'NCL-TEST-LUA-001',
    NULL,
    'SUSPECT',
    DATE_SUB(NOW(), INTERVAL 5 DAY),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 5 DAY),
    70,
    'Số lượt quét cao (3 lượt trong 24 giờ); Khoảng cách không hợp lý: 625km trong 20 phút'
FROM DUAL
ON DUPLICATE KEY UPDATE
    status = 'SUSPECT',
    suspicion_score = 70,
    suspicion_reason = 'Số lượt quét cao (3 lượt trong 24 giờ); Khoảng cách không hợp lý: 625km trong 20 phút';

-- 5. Ba lượt quét trong vòng 40 phút (vượt ngưỡng maxScansPerHour = 2 của loại Lúa)
-- Lượt 1: T - 40 phút tại Hà Nội (21.0285, 105.8542)
INSERT INTO trace_code_scan_logs
    (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal, abnormal_reason)
SELECT
    '00000000-0000-0000-0000-000930000001',
    '00000000-0000-0000-0000-000900000020',
    DATE_SUB(NOW(), INTERVAL 40 MINUTE),
    '113.160.12.34',
    'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1',
    21.0285000,
    105.8542000,
    'Hà Nội - Điểm quét bán lẻ 1',
    FALSE,
    NULL
FROM DUAL
ON DUPLICATE KEY UPDATE
    scanned_at = DATE_SUB(NOW(), INTERVAL 40 MINUTE),
    location = 'Hà Nội - Điểm quét bán lẻ 1';

-- Lượt 2: T - 20 phút tại Đà Nẵng (16.0544, 108.2022) (~625km từ Hà Nội sau 20 phút)
INSERT INTO trace_code_scan_logs
    (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal, abnormal_reason)
SELECT
    '00000000-0000-0000-0000-000930000002',
    '00000000-0000-0000-0000-000900000020',
    DATE_SUB(NOW(), INTERVAL 20 MINUTE),
    '14.248.56.78',
    'Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    16.0544000,
    108.2022000,
    'Đà Nẵng - Điểm quét siêu thị',
    FALSE,
    NULL
FROM DUAL
ON DUPLICATE KEY UPDATE
    scanned_at = DATE_SUB(NOW(), INTERVAL 20 MINUTE),
    location = 'Đà Nẵng - Điểm quét siêu thị';

-- Lượt 3: T - 0 phút tại TP. Hồ Chí Minh (10.8231, 106.6297) (~600km từ Đà Nẵng sau 20 phút)
INSERT INTO trace_code_scan_logs
    (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal, abnormal_reason)
SELECT
    '00000000-0000-0000-0000-000930000003',
    '00000000-0000-0000-0000-000900000020',
    NOW(),
    '171.244.90.12',
    'Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148',
    10.8231000,
    106.6297000,
    'TP. Hồ Chí Minh - Điểm quét chợ đầu mối',
    FALSE,
    NULL
FROM DUAL
ON DUPLICATE KEY UPDATE
    scanned_at = NOW(),
    location = 'TP. Hồ Chí Minh - Điểm quét chợ đầu mối';


