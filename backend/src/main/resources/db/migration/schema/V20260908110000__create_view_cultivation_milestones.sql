-- ============================================================
-- V20260908110000: Create compatibility view cultivation_milestones
-- Compatible with plural and singular references
-- ============================================================

CREATE OR REPLACE VIEW cultivation_milestones AS SELECT * FROM cultivation_milestone;
