-- ============================================================
-- V20260830130000: Add unlock and verification fields to trace_codes
-- for NCL-08-CN-013: Unlock trace code after verification
-- ============================================================

ALTER TABLE trace_codes
    ADD COLUMN unlocked_at TIMESTAMP NULL,
    ADD COLUMN unlocked_by CHAR(36) NULL,
    ADD COLUMN unlock_conclusion TEXT NULL,
    ADD COLUMN unlock_evidence TEXT NULL,
    ADD COLUMN verification_note TEXT NULL;

-- Foreign key for unlocked_by -> users (PK is user_id)
ALTER TABLE trace_codes
    ADD CONSTRAINT fk_trace_codes_unlocked_by
        FOREIGN KEY (unlocked_by) REFERENCES users(user_id);
