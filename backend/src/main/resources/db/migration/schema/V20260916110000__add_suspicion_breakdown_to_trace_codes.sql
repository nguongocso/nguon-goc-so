-- ============================================================
-- V20260916110000: Add suspicion breakdown snapshot fields to trace_codes
-- P1.2: Snapshot of score breakdown and violating scan logs at evaluation time
-- ============================================================

ALTER TABLE trace_codes
    ADD COLUMN high_frequency_score INT DEFAULT 0,
    ADD COLUMN impossible_travel_score INT DEFAULT 0,
    ADD COLUMN multiple_locations_score INT DEFAULT 0,
    ADD COLUMN evaluated_at TIMESTAMP NULL,
    ADD COLUMN violating_scan_log_ids TEXT NULL;
