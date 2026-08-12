-- ============================================================
-- V21: Add suspect/lock fields to trace_codes
-- for NCL-08-CN-007: Suspect trace code lock
-- ============================================================

ALTER TABLE trace_codes
    ADD COLUMN suspicion_score INT DEFAULT 0,
    ADD COLUMN suspicion_reason TEXT NULL,
    ADD COLUMN locked_at TIMESTAMP NULL,
    ADD COLUMN locked_by CHAR(36) NULL,
    ADD COLUMN lock_reason TEXT NULL;

-- Update existing status column to support new values (MySQL VARCHAR is fine, no change needed)
-- Ensure status can hold SUSPECT and LOCKED values

-- Indexes for fast querying
CREATE INDEX idx_trace_codes_status ON trace_codes(status);
CREATE INDEX idx_trace_codes_suspicion_score ON trace_codes(suspicion_score);

-- Foreign key for locked_by -> users
ALTER TABLE trace_codes
    ADD CONSTRAINT fk_trace_codes_locked_by
        FOREIGN KEY (locked_by) REFERENCES users(id);