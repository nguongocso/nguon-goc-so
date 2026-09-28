-- ============================================================
-- V20260918111000: Loại bỏ khóa thử nghiệm mẫu cứng nks_test_sample_key_1234567890
-- Yêu cầu đối tác phải sử dụng khóa thử nghiệm do Quản lý Hợp tác xã cấp (NCL-12-CN-004)
-- ============================================================

DELETE FROM partner_api_keys WHERE id = '00000000-0000-0000-0000-000900000001';
