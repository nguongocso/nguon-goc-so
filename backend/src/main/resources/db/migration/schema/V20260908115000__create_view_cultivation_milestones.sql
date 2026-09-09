-- ============================================================
-- V20260908115000: Create compatibility view cultivation_milestones
-- Compatible with plural and singular references
--
-- LƯU Ý: version 20260908110000 đã bị trùng với migration
-- "add lookup hash to product feedbacks" (NCL-922) nên view này
-- được đổi tên sang 20260908115000. View được xóa sau bởi
-- V20260908160000 khi bảng được đổi tên sang số nhiều.
-- ============================================================

CREATE OR REPLACE VIEW cultivation_milestones AS SELECT * FROM cultivation_milestone;
