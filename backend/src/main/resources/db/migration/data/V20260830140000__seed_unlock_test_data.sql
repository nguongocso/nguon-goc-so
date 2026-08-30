-- ============================================================
-- V20260830140000: Seed data for testing NCL-08-CN-013 (Unlock trace code after verification)
-- Idempotent: Fixed UUIDs + INSERT IGNORE
-- ============================================================

-- 1. Seed second Platform Admin user (admin2) to test Same Admin Rule vs Different Admin
INSERT IGNORE INTO users (user_id, user_name, password_hash, full_name, phone, email, status, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000099',
    'admin2',
    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
    'Phó Quản trị viên hệ thống',
    '0912000099',
    'admin2@system.test',
    'ACTIVE',
    NOW(),
    NOW()
);

-- Link admin2 to SYSTEM organization with VT-01 (ADMIN) role
INSERT IGNORE INTO organization_users (id, organization_id, user_id, role_id, custom_permissions, joined_at, status)
SELECT
    '00000000-0000-0000-0000-000000000098',
    (SELECT organization_id FROM organizations WHERE code = 'SYSTEM' LIMIT 1),
    '00000000-0000-0000-0000-000000000099',
    (SELECT role_id FROM roles WHERE code = 'VT-01' LIMIT 1),
    NULL,
    NOW(),
    'ACTIVE'
WHERE EXISTS (SELECT 1 FROM organizations WHERE code = 'SYSTEM')
  AND EXISTS (SELECT 1 FROM users WHERE user_id = '00000000-0000-0000-0000-000000000099')
  AND EXISTS (SELECT 1 FROM roles WHERE code = 'VT-01')
  AND NOT EXISTS (
    SELECT 1 FROM organization_users
    WHERE user_id = '00000000-0000-0000-0000-000000000099'
);

-- 2. Seed Test Organization (HTX Nông Nghiệp Xanh An Toàn)
INSERT IGNORE INTO organizations (organization_id, name, code, type, status, address, phone, email, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000080',
    'HTX Nông Nghiệp Xanh An Toàn',
    'HTX_XANH_AN_TOAN',
    'COOPERATIVE',
    'ACTIVE',
    'Xã Hòa Bình, Huyện Thường Tín, TP. Hà Nội',
    '0988776655',
    'contact@xanhantoan.test',
    NOW(),
    NOW()
);

-- 3. Seed Production Lots for testing
INSERT IGNORE INTO production_lot
    (id, organization_id, farm_area_id, product_category_id, name, expected_quantity,
     expected_quantity_unit, actual_quantity, planting_date, harvest_date, status,
     approval_notes, created_by, approved_by, created_at, updated_at)
VALUES
(
    '00000000-0000-0000-0000-000000000081',
    '00000000-0000-0000-0000-000000000080',
    NULL,
    '00000000-0000-0000-0000-000800000001',
    'Lô Nho Ninh Thuận Xuất Khẩu 2026-A1',
    5000,
    'KG',
    4850,
    '2026-03-01',
    '2026-08-15',
    'APPROVED',
    'Đã phê duyệt đạt chuẩn VietGAP',
    (SELECT user_id FROM users WHERE user_name = 'eventrecorder' LIMIT 1),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000000000082',
    '00000000-0000-0000-0000-000000000080',
    NULL,
    '00000000-0000-0000-0000-000800000005',
    'Lô Sầu Riêng Ri6 Đắk Lắk 2026-B2',
    8000,
    'KG',
    7900,
    '2026-02-10',
    '2026-08-20',
    'APPROVED',
    'Đã phê duyệt đạt chuẩn kiểm nghiệm',
    (SELECT user_id FROM users WHERE user_name = 'eventrecorder' LIMIT 1),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    NOW(),
    NOW()
);

-- 4. Seed Shipments linked to lots
INSERT IGNORE INTO shipments
    (id, production_lot_id, organization_id, name, total_quantity, packaging_info, status, created_by, created_at, updated_at)
VALUES
(
    '00000000-0000-0000-0000-000000000083',
    '00000000-0000-0000-0000-000000000081',
    '00000000-0000-0000-0000-000000000080',
    'Lô hàng Nho xuất khẩu Siêu thị Miền Bắc',
    2500,
    'Thùng carton 5kg có đệm khí',
    'ACTIVATED',
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000000000084',
    '00000000-0000-0000-0000-000000000082',
    '00000000-0000-0000-0000-000000000080',
    'Lô hàng Sầu Riêng phân phối Toàn quốc',
    4000,
    'Thùng gỗ 20kg chuyên dụng',
    'ACTIVATED',
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    NOW(),
    NOW()
);

-- 5. Seed Trace Codes with various statuses:
-- 5.1. 5 LOCKED codes locked by admin
INSERT IGNORE INTO trace_codes
    (id, shipment_id, code_value, qr_image, status, activated_at, activated_by, created_at,
     suspicion_score, suspicion_reason, locked_at, locked_by, lock_reason)
SELECT
    CONCAT('00000000-0000-0000-0000-00000001000', t.idx),
    '00000000-0000-0000-0000-000000000083',
    CONCAT('NCL-TEST-LCK-ADM1-', LPAD(t.idx, 2, '0')),
    CONCAT('/files/qr/NCL-TEST-LCK-ADM1-', LPAD(t.idx, 2, '0'), '.png'),
    'LOCKED',
    DATE_SUB(NOW(), INTERVAL 5 DAY),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 5 DAY),
    t.score,
    t.susp_reason,
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1),
    t.lck_reason
FROM (
    SELECT 1 idx, 85 score, 'Quét 15 lượt trong 24h từ 5 vị trí địa lý khác nhau' susp_reason, 'Phát hiện quét bất thường đồng thời tại Hà Nội và Cần Thơ' lck_reason
    UNION ALL SELECT 2, 90, 'Khoảng cách di chuyển bất khả thi >500km trong 15 phút', 'Nghi vấn tem bị sao chép hoặc in lậu tại nhiều đại lý'
    UNION ALL SELECT 3, 75, 'Tần suất quét tăng đột biến vượt ngưỡng 20 lượt/ngày', 'Nhiều người tiêu dùng phản ánh quét mã ra cùng một vị trí lạ'
    UNION ALL SELECT 4, 80, 'Quét đồng thời từ nhiều địa chỉ IP không xác định', 'Cảnh báo tự động từ hệ thống giám sát an ninh quét mã'
    UNION ALL SELECT 5, 95, 'Mã tem bị quét lặp lại liên tục từ các thiết bị lạ', 'Phát hiện dấu hiệu gian lận tem nhãn tại chuỗi phân phối'
) t;

-- 5.2. 3 LOCKED codes locked by admin2
INSERT IGNORE INTO trace_codes
    (id, shipment_id, code_value, qr_image, status, activated_at, activated_by, created_at,
     suspicion_score, suspicion_reason, locked_at, locked_by, lock_reason)
SELECT
    CONCAT('00000000-0000-0000-0000-00000002000', t.idx),
    '00000000-0000-0000-0000-000000000084',
    CONCAT('NCL-TEST-LCK-ADM2-', LPAD(t.idx, 2, '0')),
    CONCAT('/files/qr/NCL-TEST-LCK-ADM2-', LPAD(t.idx, 2, '0'), '.png'),
    'LOCKED',
    DATE_SUB(NOW(), INTERVAL 7 DAY),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 7 DAY),
    t.score,
    t.susp_reason,
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    '00000000-0000-0000-0000-000000000099',
    t.lck_reason
FROM (
    SELECT 1 idx, 80 score, 'Quét tại 4 thành phố khác nhau trong vòng 1 giờ' susp_reason, 'Khóa tạm thời chờ đối soát chứng từ xuất kho HTX' lck_reason
    UNION ALL SELECT 2, 70, 'Tần suất quét cao từ mạng di động lạ', 'Khóa để xác minh nguồn gốc phân phối lô hàng'
    UNION ALL SELECT 3, 85, 'Quét lặp lại 18 lượt tại khu vực chợ đầu mối', 'Khóa do nghi ngờ tem bị photocopy dán trên sản phẩm khác'
) t;

-- 5.3. 5 ACTIVE normal trace codes
INSERT IGNORE INTO trace_codes
    (id, shipment_id, code_value, qr_image, status, activated_at, activated_by, created_at,
     suspicion_score, suspicion_reason)
SELECT
    CONCAT('00000000-0000-0000-0000-00000003000', t.idx),
    '00000000-0000-0000-0000-000000000083',
    CONCAT('NCL-TEST-ACT-', LPAD(t.idx, 2, '0')),
    CONCAT('/files/qr/NCL-TEST-ACT-', LPAD(t.idx, 2, '0'), '.png'),
    'ACTIVE',
    DATE_SUB(NOW(), INTERVAL 3 DAY),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 3 DAY),
    0,
    NULL
FROM (
    SELECT 1 idx UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) t;

-- 5.4. 3 INACTIVE trace codes (not yet activated)
INSERT IGNORE INTO trace_codes
    (id, shipment_id, code_value, qr_image, status, activated_at, activated_by, created_at,
     suspicion_score, suspicion_reason)
SELECT
    CONCAT('00000000-0000-0000-0000-00000004000', t.idx),
    '00000000-0000-0000-0000-000000000084',
    CONCAT('NCL-TEST-INA-', LPAD(t.idx, 2, '0')),
    CONCAT('/files/qr/NCL-TEST-INA-', LPAD(t.idx, 2, '0'), '.png'),
    'INACTIVE',
    NULL,
    NULL,
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    0,
    NULL
FROM (
    SELECT 1 idx UNION ALL SELECT 2 UNION ALL SELECT 3
) t;

-- 5.5. 2 PREVIOUSLY UNLOCKED trace codes (ACTIVE status with verification metadata)
INSERT IGNORE INTO trace_codes
    (id, shipment_id, code_value, qr_image, status, activated_at, activated_by, created_at,
     suspicion_score, suspicion_reason, locked_at, locked_by, lock_reason,
     unlocked_at, unlocked_by, unlock_conclusion, unlock_evidence, verification_note)
SELECT
    CONCAT('00000000-0000-0000-0000-00000005000', t.idx),
    '00000000-0000-0000-0000-000000000083',
    CONCAT('NCL-TEST-UNLOCKED-', LPAD(t.idx, 2, '0')),
    CONCAT('/files/qr/NCL-TEST-UNLOCKED-', LPAD(t.idx, 2, '0'), '.png'),
    'ACTIVE',
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    (SELECT user_id FROM users WHERE user_name = 'orgmanager' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    40,
    'Tần suất quét tăng tạm thời tại hội chợ thương mại',
    DATE_SUB(NOW(), INTERVAL 3 DAY),
    (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1),
    'Khóa tạm thời để kiểm tra nguồn quét hội chợ',
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1),
    t.conclusion,
    t.evidence,
    t.conclusion
FROM (
    SELECT 1 idx, 'Đã xác minh tem quét tại quầy trưng bày Hội chợ Nông sản Quốc tế 2026, sản phẩm chính hãng.' conclusion, 'Biên bản làm việc số 15/BB-HC ngày 28/08/2026 đính kèm' evidence
    UNION ALL SELECT 2, 'Đã đối soát hóa đơn bán lẻ và danh sách xuất kho tại chuỗi siêu thị WinMart, xác nhận tem hợp lệ.' conclusion, 'Hóa đơn GTGT điện tử số HD-0098231' evidence
) t;

-- 6. Seed Scan Logs for locked codes
INSERT IGNORE INTO trace_code_scan_logs
    (id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal, abnormal_reason)
VALUES
(
    UUID(),
    '00000000-0000-0000-0000-000000010001',
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    '113.190.234.12',
    'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Mobile/15E148',
    21.0285110,
    105.8542000,
    'Quận Hoàn Kiếm, TP. Hà Nội',
    FALSE,
    NULL
),
(
    UUID(),
    '00000000-0000-0000-0000-000000010001',
    DATE_SUB(NOW(), INTERVAL 46 HOUR),
    '14.161.22.88',
    'Mozilla/5.0 (Linux; Android 14; SM-S918B) Mobile Safari/537.36',
    10.0452000,
    105.7469000,
    'Quận Ninh Kiều, TP. Cần Thơ',
    TRUE,
    'Khoảng cách quét bất thường trong thời gian ngắn'
),
(
    UUID(),
    '00000000-0000-0000-0000-000000010002',
    DATE_SUB(NOW(), INTERVAL 3 DAY),
    '115.79.44.10',
    'Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X)',
    16.0544000,
    108.2022000,
    'Quận Hải Châu, TP. Đà Nẵng',
    TRUE,
    'Quét bất thường nhiều vị trí'
);
