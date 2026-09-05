-- ============================================================
-- Fix: Unique constraint on inspection_criteria should be based on
-- criterion_id (the identity of the criterion in the catalog),
-- NOT criterion_code (the display name).
--
-- Business rule:
--   - Two criteria with DIFFERENT IDs but SAME name are NOT duplicates.
--   - Two criteria with SAME ID are duplicates.
--
-- Before: UNIQUE (inspection_request_id, criterion_code)
-- After:  UNIQUE (inspection_request_id, criterion_id)
--
-- Note:
--   The old unique index is also used by the foreign key
--   fk_inspection_criterion_request on inspection_request_id.
--   Therefore, create a dedicated index for the FK first.
-- ============================================================

ALTER TABLE inspection_criteria
    ADD INDEX idx_inspection_criteria_request (inspection_request_id);

ALTER TABLE inspection_criteria
DROP INDEX uk_inspection_request_criterion;

ALTER TABLE inspection_criteria
    ADD CONSTRAINT uk_inspection_request_criterion
        UNIQUE (inspection_request_id, criterion_id);

