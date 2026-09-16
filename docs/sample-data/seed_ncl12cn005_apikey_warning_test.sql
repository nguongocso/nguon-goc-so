-- ============================================================
-- Seed dữ liệu kiểm thử thủ công NCL-12-CN-005:
-- "Cảnh báo khóa truy cập sắp hết hạn và sắp chạm hạn mức".
-- Chạy trên MySQL dev (127.0.0.1:3306, db: nguon_goc_so).
--
-- Mục đích: tạo 4 khóa truy cập + 1 thông báo mẫu thuộc tổ chức
-- HTXA (người nhận/kiểm thử: managerA, vai trò VT-02) để kiểm tra:
--   TC-01: khóa còn 5 ngày (trong ngưỡng 7 ngày) -> phải cảnh báo
--          hết hạn kèm lối tắt gia hạn.
--   TC-02: khóa thử nghiệm hạn mức 10 lượt/giờ -> gọi 8 lượt (80%)
--          phải cảnh báo sắp chạm hạn mức; gọi lượt 11 bị 429.
--   TC-03: khóa đã thu hồi -> không tạo cảnh báo khi quét.
--   TC-04: đã có 1 thông báo trong ngày cho khóa TC-01 -> quét lần 2
--          không tạo trùng.
-- Đối chứng: 1 khóa bình thường (hết hạn sau 365 ngày, hạn mức cao)
--   -> không cảnh báo.
--
-- Idempotent: INSERT IGNORE + UUID cố định, chạy lại an toàn.
-- File này KHÔNG commit lên CI (quy ước docs/sample-data/).
--
-- Cách chạy (PowerShell, tại thư mục gốc repo):
--   & "C:\xampp\mysql\bin\mysql.exe" -h 127.0.0.1 -P 3306 -u root `
--     nguon_goc_so < docs\sample-data\seed_ncl12cn005_apikey_warning_test.sql
-- ============================================================

-- ============================================================
-- Dọn thông báo cũ của các khóa test trước khi gieo lại dữ liệu.
-- Lý do: nội dung thông báo là ảnh chụp (snapshot) tại thời điểm gửi —
-- tên đối tác, số lượt gọi, ngày hết hạn được ghép sẵn vào chuỗi content.
-- Chạy lại seed (đổi tên đối tác, reset ngày hết hạn) mà giữ lại các
-- thông báo cũ sẽ khiến chuông hiển thị dữ liệu test/stale, lệch với trang
-- Cảnh báo tổng hợp (/alerts) vốn tính real-time từ dữ liệu hiện tại.
-- ============================================================
DELETE FROM notifications
WHERE entity_id IN ('00000000-0000-0000-0000-001200000001',
                    '00000000-0000-0000-0000-001200000002',
                    '00000000-0000-0000-0000-001200000003',
                    '00000000-0000-0000-0000-001200000004');


-- ============================================================
-- TC-01: Khóa sắp hết hạn (còn 5 ngày, trong ngưỡng 7 ngày)
-- RAW KEY (gửi header X-API-KEY khi gọi cổng đối tác):
--   nks_live_710exp01aabbccddeeff00112233445566778899aabbccddeeff01
-- ============================================================
INSERT IGNORE INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-001200000001',
    (SELECT organization_id FROM organizations WHERE code = 'HTXA' LIMIT 1),
    'Công ty TNHH Nông sản Bình Minh',
    'nks_live_710exp01',
    SHA2('nks_live_710exp01aabbccddeeff00112233445566778899aabbccddeeff01', 256),
    100,
    DATE_ADD(NOW(), INTERVAL 5 DAY),
    'ACTIVE',
    FALSE,
    0,
    0,
    (SELECT user_id FROM users WHERE user_name = 'managerA' LIMIT 1),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE code = 'HTXA')
  AND EXISTS (SELECT 1 FROM users WHERE user_name = 'managerA');

-- ============================================================
-- TC-02: Khóa sắp chạm hạn mức (khóa thử nghiệm, 10 lượt/giờ;
--   8 lượt gọi thành công = 80% -> ngưỡng cảnh báo)
-- RAW KEY (gửi header X-API-KEY khi gọi cổng đối tác):
--   nks_test_710qta02aabbccddeeff00112233445566778899aabbccddeeff02
-- Gợi ý bắn 8 lượt mẫu (trả dữ liệu sandbox, không cần lô thật):
--   curl.exe -H "X-API-KEY: nks_test_710qta02aabbccddeeff00112233445566778899aabbccddeeff02" ^
--     http://localhost:8080/api/v1/partner/trace/TEST-TRACE-001
-- ============================================================
INSERT IGNORE INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-001200000002',
    (SELECT organization_id FROM organizations WHERE code = 'HTXA' LIMIT 1),
    'Hợp tác xã Cà phê Tân Cương',
    'nks_test_710qta02',
    SHA2('nks_test_710qta02aabbccddeeff00112233445566778899aabbccddeeff02', 256),
    10,
    DATE_ADD(NOW(), INTERVAL 25 DAY),
    'ACTIVE',
    TRUE,
    0,
    0,
    (SELECT user_id FROM users WHERE user_name = 'managerA' LIMIT 1),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE code = 'HTXA')
  AND EXISTS (SELECT 1 FROM users WHERE user_name = 'managerA');

-- ============================================================
-- TC-03: Khóa đã thu hồi (quét cảnh báo phải bỏ qua)
-- RAW KEY (dùng để kiểm tra gọi bị từ chối 401):
--   nks_live_710rev03aabbccddeeff00112233445566778899aabbccddeeff03
-- ============================================================
INSERT IGNORE INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls,
     created_by, created_at, revoked_by, revoked_at)
SELECT
    '00000000-0000-0000-0000-001200000003',
    (SELECT organization_id FROM organizations WHERE code = 'HTXA' LIMIT 1),
    'Công ty Cổ phần Chế biến Gia vị Đại Việt',
    'nks_live_710rev03',
    SHA2('nks_live_710rev03aabbccddeeff00112233445566778899aabbccddeeff03', 256),
    100,
    DATE_ADD(NOW(), INTERVAL 20 DAY),
    'REVOKED',
    FALSE,
    0,
    0,
    (SELECT user_id FROM users WHERE user_name = 'managerA' LIMIT 1),
    NOW(),
    (SELECT user_id FROM users WHERE user_name = 'managerA' LIMIT 1),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE code = 'HTXA')
  AND EXISTS (SELECT 1 FROM users WHERE user_name = 'managerA');

-- ============================================================
-- Đối chứng: khóa bình thường (hết hạn sau 365 ngày, 1000 lượt/giờ)
--   -> không thuộc diện cảnh báo.
-- RAW KEY:
--   nks_live_710ok04aabbccddeeff00112233445566778899aabbccddeeff04
-- ============================================================
INSERT IGNORE INTO partner_api_keys
    (id, organization_id, partner_name, key_prefix, key_hash, rate_limit_per_hour,
     expires_at, status, is_test, total_calls, failed_calls, created_by, created_at)
SELECT
    '00000000-0000-0000-0000-001200000004',
    (SELECT organization_id FROM organizations WHERE code = 'HTXA' LIMIT 1),
    'Hợp tác xã Rau an toàn Sơn La',
    'nks_live_710ok04',
    SHA2('nks_live_710ok04aabbccddeeff00112233445566778899aabbccddeeff04', 256),
    1000,
    DATE_ADD(NOW(), INTERVAL 365 DAY),
    'ACTIVE',
    FALSE,
    0,
    0,
    (SELECT user_id FROM users WHERE user_name = 'managerA' LIMIT 1),
    NOW()
WHERE EXISTS (SELECT 1 FROM organizations WHERE code = 'HTXA')
  AND EXISTS (SELECT 1 FROM users WHERE user_name = 'managerA');

-- ============================================================
-- TC-04: Thông báo "đã cảnh báo trong ngày" cho khóa TC-01
-- (mô phỏng trạng thái sau lần quét đầu; lần quét thứ 2 trong
-- ngày phải KHÔNG tạo thêm). entity_id trỏ tới khóa TC-01 để
-- nút thông báo mở được popup chi tiết + lối tắt Xem khóa.
-- ============================================================
INSERT IGNORE INTO notifications
    (id, user_id, type, title, content, entity_id, is_read, read_at, created_at)
SELECT
    '00000000-0000-0000-0000-002200000001',
    (SELECT user_id FROM users WHERE user_name = 'managerA' LIMIT 1),
    'ALERT',
    'Khóa truy cập sắp hết hạn',
    CONCAT('Khóa truy cập của đối tác "Công ty TNHH Nông sản Bình Minh" sẽ hết hạn sau 5 ngày (vào ',
           DATE_FORMAT(DATE_ADD(NOW(), INTERVAL 5 DAY), '%d/%m/%Y %H:%i'),
           '). Vui lòng gia hạn để đối tác không bị gián đoạn kết nối.'),
    '00000000-0000-0000-0000-001200000001',
    FALSE,
    NULL,
    NOW()
WHERE EXISTS (SELECT 1 FROM users WHERE user_name = 'managerA')
  AND EXISTS (SELECT 1 FROM partner_api_keys WHERE id = '00000000-0000-0000-0000-001200000001');

-- ============================================================
-- Kiểm chứng sau khi chạy (các câu lệnh SELECT tham khảo):
-- SELECT id, partner_name, key_prefix, rate_limit_per_hour,
--        expires_at, DATEDIFF(expires_at, NOW()) AS days_left,
--        status, is_test
-- FROM partner_api_keys
-- WHERE id IN ('00000000-0000-0000-0000-001200000001',
--              '00000000-0000-0000-0000-001200000002',
--              '00000000-0000-0000-0000-001200000003',
--              '00000000-0000-0000-0000-001200000004');
-- SELECT id, type, title, entity_id, is_read, created_at
-- FROM notifications
-- WHERE id = '00000000-0000-0000-0000-002200000001';
-- ============================================================
