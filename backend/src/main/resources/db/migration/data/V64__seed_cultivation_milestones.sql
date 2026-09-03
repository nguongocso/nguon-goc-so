-- ============================================================
-- V64: Seed cultivation milestone catalog samples + assignments
-- Story: NCL-09-CN-011
-- ============================================================

-- 1. Seed milestone catalog (4 baseline milestones)
INSERT IGNORE INTO cultivation_milestone_catalog (id, name, description, activity_type, expected_days_from_planting, status, created_at)
VALUES
    (1, 'Gieo trồng ban đầu', 'Tiến hành gieo trồng lô hàng theo quy trình', 'PLANTING', 0, 'ACTIVE', NOW()),
    (2, 'Bón phân đợt 1', 'Bón phân lót trước khi gieo hoặc ngay sau gieo', 'FERTILIZING', 7, 'ACTIVE', NOW()),
    (3, 'Phun thuốc phòng ngừa', 'Phun thuốc bảo vệ thực vật phòng ngừa sâu bệnh', 'PESTICIDE', 21, 'ACTIVE', NOW()),
    (4, 'Thu hoạch', 'Thu hoạch sản phẩm khi đạt yêu cầu', 'HARVESTING', 90, 'ACTIVE', NOW());

-- 2. Assign milestones to product categories (demo) — chỉ gán cho 2 category đầu
-- Assign GLOBAL milestones (standard_id = NULL) cho tối đa 2 active product categories
INSERT IGNORE INTO product_category_milestones (id, category_id, milestone_id, standard_id, is_mandatory)
SELECT
    UUID(),
    pc.id,
    cm.id,
    NULL,
    TRUE
FROM (
    SELECT id FROM product_categories
    WHERE is_active = TRUE
    ORDER BY name ASC
    LIMIT 2
) pc
CROSS JOIN cultivation_milestone_catalog cm
WHERE cm.status = 'ACTIVE'
  AND cm.id IN (1, 2, 3, 4);
