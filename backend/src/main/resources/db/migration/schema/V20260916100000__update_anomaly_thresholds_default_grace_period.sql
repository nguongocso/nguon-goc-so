-- ============================================================
-- V20260916100000: Update default activation_age_days to 3 days (P1.1)
-- Grace period during which anomaly detection is skipped after activation
-- ============================================================

UPDATE anomaly_thresholds
SET activation_age_days = 3
WHERE product_category_id IS NULL AND activation_age_days = 365;
