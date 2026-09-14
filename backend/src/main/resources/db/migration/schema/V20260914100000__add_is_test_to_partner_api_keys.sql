-- ============================================================
-- V20260914100000: Bổ sung trường khóa thử nghiệm (is_test) vào bảng partner_api_keys
-- User Story: NCL-12-CN-004 - Trang tài liệu cổng dữ liệu và khóa thử nghiệm
-- ============================================================

ALTER TABLE partner_api_keys
    ADD COLUMN is_test BOOLEAN NOT NULL DEFAULT FALSE;
