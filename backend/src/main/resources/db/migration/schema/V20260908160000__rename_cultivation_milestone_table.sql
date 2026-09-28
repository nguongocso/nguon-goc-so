-- ============================================================
-- V20260908160000: Rename table cultivation_milestone to cultivation_milestones
-- (plural convention)
-- ============================================================

-- 1. Xóa view tạm nếu đã tạo trước đó để tránh trùng tên khi rename
DROP VIEW IF EXISTS cultivation_milestones;

-- 2. Đổi tên bảng từ số ít sang số nhiều
RENAME TABLE cultivation_milestone TO cultivation_milestones;
