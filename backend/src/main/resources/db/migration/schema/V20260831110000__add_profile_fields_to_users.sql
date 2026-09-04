-- ============================================================
-- V20260831100000: Bổ sung avatar_url và ràng buộc unique email cho bảng users
-- User Story: NCL-01-CN-010
-- ============================================================

ALTER TABLE users
    ADD COLUMN avatar_url VARCHAR(500) NULL AFTER email;

ALTER TABLE users
    ADD CONSTRAINT uk_users_email UNIQUE (email);
