-- ============================================================
-- V20260908115000: Create compatibility view cultivation_milestones
-- Compatible with plural and singular references
--
-- LƯU Ý: version 20260908110000 đã bị trùng với migration
-- "add lookup hash to product feedbacks" (NCL-922) nên view này
-- được đổi tên sang 20260908115000. View được xóa sau bởi
-- V20260908160000 khi bảng được đổi tên sang số nhiều.
-- ============================================================

-- Chỉ tạo view khi đối tượng cultivation_milestones chưa phải là BASE TABLE
-- (Tránh lỗi ERROR 1347 khi V20260908160000 đã chạy đổi tên bảng trước đó)
SET @is_table = (
    SELECT COUNT(*) 
    FROM information_schema.tables 
    WHERE table_schema = DATABASE() 
      AND table_name = 'cultivation_milestones' 
      AND table_type = 'BASE TABLE'
);

SET @sql = IF(@is_table > 0, 
    'SELECT 1', 
    'CREATE OR REPLACE VIEW cultivation_milestones AS SELECT * FROM cultivation_milestone'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
