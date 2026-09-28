-- ============================================================
-- V20260907160005: Seed scan logs
-- Phase 6: TraceCodeScanLog (38 lượt quét cho các mã ACTIVE)
--
-- Đặc điểm: Idempotent (INSERT IGNORE + Deterministic UUIDs)
-- ============================================================

INSERT IGNORE INTO trace_code_scan_logs (
    id, trace_code_id, scanned_at, ip_address, user_agent, latitude, longitude, location, is_abnormal, abnormal_reason
) VALUES
('00000000-0000-0000-0000-000d00000001', (SELECT id FROM trace_codes WHERE code_value = '893001000036' LIMIT 1), '2026-08-15 08:30:00', '14.162.180.10', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)', 21.0285, 105.8542, 'Hà Nội', FALSE, NULL),
('00000000-0000-0000-0000-000d00000002', (SELECT id FROM trace_codes WHERE code_value = '893001000036' LIMIT 1), '2026-08-15 08:35:00', '113.161.72.15', 'Mozilla/5.0 (Linux; Android 14)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', TRUE, 'Khoảng cách di chuyển bất khả thi giữa 2 lần quét (< 15 phút giữa Hà Nội và TP.HCM)'),
('00000000-0000-0000-0000-000d00000003', (SELECT id FROM trace_codes WHERE code_value = '893001000036' LIMIT 1), '2026-08-18 10:15:00', '42.112.35.20', 'Mozilla/5.0 (iPhone)', 16.0544, 108.2022, 'Đà Nẵng', FALSE, NULL),
('00000000-0000-0000-0000-000d00000004', (SELECT id FROM trace_codes WHERE code_value = '893001000036' LIMIT 1), '2026-08-22 14:00:00', '115.79.140.5', 'Mozilla/5.0 (Linux; Android)', 10.0452, 105.7469, 'Cần Thơ', FALSE, NULL),
('00000000-0000-0000-0000-000d00000005', (SELECT id FROM trace_codes WHERE code_value = '893001000036' LIMIT 1), '2026-08-28 16:45:00', '113.161.72.88', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', FALSE, NULL),
('00000000-0000-0000-0000-000d00000006', (SELECT id FROM trace_codes WHERE code_value = '893001000037' LIMIT 1), '2026-08-12 09:00:00', '14.162.180.22', 'Mozilla/5.0 (iPhone)', 21.0285, 105.8542, 'Hà Nội', FALSE, NULL),
('00000000-0000-0000-0000-000d00000007', (SELECT id FROM trace_codes WHERE code_value = '893001000037' LIMIT 1), '2026-08-12 09:01:00', '14.162.180.22', 'Mozilla/5.0 (iPhone)', 21.0285, 105.8542, 'Hà Nội', TRUE, 'Tần suất quét liên tục vượt quá ngưỡng an toàn'),
('00000000-0000-0000-0000-000d00000008', (SELECT id FROM trace_codes WHERE code_value = '893001000037' LIMIT 1), '2026-08-19 11:20:00', '113.161.72.33', 'Mozilla/5.0 (Linux; Android)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', FALSE, NULL),
('00000000-0000-0000-0000-000d00000009', (SELECT id FROM trace_codes WHERE code_value = '893001000037' LIMIT 1), '2026-08-25 15:10:00', '113.161.72.44', 'Mozilla/5.0 (iPhone)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', FALSE, NULL),
('00000000-0000-0000-0000-000d00000010', (SELECT id FROM trace_codes WHERE code_value = '893001000038' LIMIT 1), '2026-08-14 08:45:00', '42.112.35.50', 'Mozilla/5.0 (iPhone)', 16.0544, 108.2022, 'Đà Nẵng', FALSE, NULL),
('00000000-0000-0000-0000-000d00000011', (SELECT id FROM trace_codes WHERE code_value = '893001000038' LIMIT 1), '2026-08-17 14:30:00', '115.79.140.60', 'Mozilla/5.0 (Linux; Android)', 10.0452, 105.7469, 'Cần Thơ', FALSE, NULL),
('00000000-0000-0000-0000-000d00000012', (SELECT id FROM trace_codes WHERE code_value = '893001000038' LIMIT 1), '2026-08-21 16:00:00', '14.162.180.70', 'Mozilla/5.0 (Windows NT 10.0)', 21.0285, 105.8542, 'Hà Nội', FALSE, NULL),
('00000000-0000-0000-0000-000d00000013', (SELECT id FROM trace_codes WHERE code_value = '893001000038' LIMIT 1), '2026-08-29 09:15:00', '42.112.35.80', 'Mozilla/5.0 (iPhone)', 16.0544, 108.2022, 'Đà Nẵng', FALSE, NULL),
('00000000-0000-0000-0000-000d00000014', (SELECT id FROM trace_codes WHERE code_value = '893001000039' LIMIT 1), '2026-08-16 10:00:00', '113.161.72.90', 'Mozilla/5.0 (Linux; Android)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', FALSE, NULL),
('00000000-0000-0000-0000-000d00000015', (SELECT id FROM trace_codes WHERE code_value = '893001000039' LIMIT 1), '2026-08-20 13:40:00', '115.79.140.12', 'Mozilla/5.0 (iPhone)', 10.0452, 105.7469, 'Cần Thơ', FALSE, NULL),
('00000000-0000-0000-0000-000d00000016', (SELECT id FROM trace_codes WHERE code_value = '893001000039' LIMIT 1), '2026-08-24 15:30:00', '115.79.140.15', 'Mozilla/5.0 (iPhone)', 10.0452, 105.7469, 'Cần Thơ', FALSE, NULL),
('00000000-0000-0000-0000-000d00000017', (SELECT id FROM trace_codes WHERE code_value = '893001000039' LIMIT 1), '2026-09-01 11:00:00', '113.161.72.95', 'Mozilla/5.0 (Linux; Android)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', FALSE, NULL),
('00000000-0000-0000-0000-000d00000018', (SELECT id FROM trace_codes WHERE code_value = '893001000040' LIMIT 1), '2026-08-13 09:30:00', '14.162.180.101', 'Mozilla/5.0 (iPhone)', 21.0285, 105.8542, 'Hà Nội', FALSE, NULL),
('00000000-0000-0000-0000-000d00000019', (SELECT id FROM trace_codes WHERE code_value = '893001000040' LIMIT 1), '2026-08-22 14:15:00', '42.112.35.102', 'Mozilla/5.0 (Linux; Android)', 16.0544, 108.2022, 'Đà Nẵng', FALSE, NULL),
('00000000-0000-0000-0000-000d00000020', (SELECT id FROM trace_codes WHERE code_value = '893001000040' LIMIT 1), '2026-08-30 17:00:00', '113.161.72.103', 'Mozilla/5.0 (iPhone)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', FALSE, NULL),
('00000000-0000-0000-0000-000d00000021', (SELECT id FROM trace_codes WHERE code_value = '893001000041' LIMIT 1), '2026-08-15 11:00:00', '113.161.72.111', 'Mozilla/5.0 (iPhone)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', FALSE, NULL),
('00000000-0000-0000-0000-000d00000022', (SELECT id FROM trace_codes WHERE code_value = '893001000041' LIMIT 1), '2026-08-23 15:45:00', '113.161.72.112', 'Mozilla/5.0 (Linux; Android)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', FALSE, NULL),
('00000000-0000-0000-0000-000d00000023', (SELECT id FROM trace_codes WHERE code_value = '893001000041' LIMIT 1), '2026-09-02 08:30:00', '14.162.180.113', 'Mozilla/5.0 (Windows NT 10.0)', 21.0285, 105.8542, 'Hà Nội', FALSE, NULL),
('00000000-0000-0000-0000-000d00000024', (SELECT id FROM trace_codes WHERE code_value = '893001000011' LIMIT 1), '2026-07-26 09:00:00', '14.162.180.121', 'Mozilla/5.0 (iPhone)', 21.0285, 105.8542, 'Hà Nội', FALSE, NULL),
('00000000-0000-0000-0000-000d00000025', (SELECT id FROM trace_codes WHERE code_value = '893001000011' LIMIT 1), '2026-08-05 10:30:00', '14.162.180.122', 'Mozilla/5.0 (iPhone)', 21.0285, 105.8542, 'Hà Nội', FALSE, NULL),
('00000000-0000-0000-0000-000d00000026', (SELECT id FROM trace_codes WHERE code_value = '893001000011' LIMIT 1), '2026-08-18 16:15:00', '42.112.35.123', 'Mozilla/5.0 (Linux; Android)', 16.0544, 108.2022, 'Đà Nẵng', FALSE, NULL),
('00000000-0000-0000-0000-000d00000027', (SELECT id FROM trace_codes WHERE code_value = '893001000012' LIMIT 1), '2026-07-28 14:00:00', '115.79.140.131', 'Mozilla/5.0 (iPhone)', 10.0452, 105.7469, 'Cần Thơ', FALSE, NULL),
('00000000-0000-0000-0000-000d00000028', (SELECT id FROM trace_codes WHERE code_value = '893001000012' LIMIT 1), '2026-08-08 11:20:00', '113.161.72.132', 'Mozilla/5.0 (Linux; Android)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', FALSE, NULL),
('00000000-0000-0000-0000-000d00000029', (SELECT id FROM trace_codes WHERE code_value = '893001000012' LIMIT 1), '2026-08-25 15:40:00', '113.161.72.133', 'Mozilla/5.0 (iPhone)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', FALSE, NULL),
('00000000-0000-0000-0000-000d00000030', (SELECT id FROM trace_codes WHERE code_value = '893001000013' LIMIT 1), '2026-07-29 08:30:00', '42.112.35.141', 'Mozilla/5.0 (iPhone)', 16.0544, 108.2022, 'Đà Nẵng', FALSE, NULL),
('00000000-0000-0000-0000-000d00000031', (SELECT id FROM trace_codes WHERE code_value = '893001000013' LIMIT 1), '2026-08-10 13:50:00', '14.162.180.142', 'Mozilla/5.0 (Linux; Android)', 21.0285, 105.8542, 'Hà Nội', FALSE, NULL),
('00000000-0000-0000-0000-000d00000032', (SELECT id FROM trace_codes WHERE code_value = '893001000013' LIMIT 1), '2026-08-27 16:30:00', '14.162.180.143', 'Mozilla/5.0 (Windows NT 10.0)', 21.0285, 105.8542, 'Hà Nội', FALSE, NULL),
('00000000-0000-0000-0000-000d00000033', (SELECT id FROM trace_codes WHERE code_value = '893001000081' LIMIT 1), '2026-08-18 09:40:00', '14.162.180.151', 'Mozilla/5.0 (iPhone)', 21.0285, 105.8542, 'Hà Nội', FALSE, NULL),
('00000000-0000-0000-0000-000d00000034', (SELECT id FROM trace_codes WHERE code_value = '893001000081' LIMIT 1), '2026-08-26 14:10:00', '113.161.72.152', 'Mozilla/5.0 (Linux; Android)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', FALSE, NULL),
('00000000-0000-0000-0000-000d00000035', (SELECT id FROM trace_codes WHERE code_value = '893001000082' LIMIT 1), '2026-08-19 10:30:00', '113.161.72.161', 'Mozilla/5.0 (iPhone)', 10.8231, 106.6297, 'TP. Hồ Chí Minh', FALSE, NULL),
('00000000-0000-0000-0000-000d00000036', (SELECT id FROM trace_codes WHERE code_value = '893001000082' LIMIT 1), '2026-08-28 15:20:00', '42.112.35.162', 'Mozilla/5.0 (Linux; Android)', 16.0544, 108.2022, 'Đà Nẵng', FALSE, NULL),
('00000000-0000-0000-0000-000d00000037', (SELECT id FROM trace_codes WHERE code_value = '893001000083' LIMIT 1), '2026-08-20 08:50:00', '115.79.140.171', 'Mozilla/5.0 (iPhone)', 10.0452, 105.7469, 'Cần Thơ', FALSE, NULL),
('00000000-0000-0000-0000-000d00000038', (SELECT id FROM trace_codes WHERE code_value = '893001000083' LIMIT 1), '2026-08-31 11:45:00', '14.162.180.172', 'Mozilla/5.0 (Linux; Android)', 21.0285, 105.8542, 'Hà Nội', FALSE, NULL);
