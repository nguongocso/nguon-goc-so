-- ============================================================
-- V20260908130000: Seed cultivation milestones for product categories
-- Story: NCL-09-CN-011, NCL-03-CN-007
-- ============================================================

-- 1. Mốc cho Cà phê (00000000-0000-0000-000800000008)
INSERT IGNORE INTO cultivation_milestone (
    name, description, activity_type, expected_days_from_planting, product_category_id, standard_id, is_mandatory, created_at
) VALUES 
('Gieo trồng cà phê', 'Gieo trồng cây giống cà phê vào hố đất', 'PLANTING', 0, '00000000-0000-0000-000800000008', NULL, TRUE, NOW()),
('Bón phân lót cà phê', 'Bón phân hữu cơ và vi sinh đợt đầu', 'FERTILIZING', 7, '00000000-0000-0000-000800000008', NULL, TRUE, NOW()),
('Phun thuốc phòng sâu cà phê', 'Phun thuốc sinh học bảo vệ lá và rễ', 'PESTICIDE', 21, '00000000-0000-0000-000800000008', NULL, TRUE, NOW()),
('Thu hoạch cà phê', 'Thu hái quả cà phê chín đạt tiêu chuẩn', 'HARVESTING', 90, '00000000-0000-0000-000800000008', NULL, TRUE, NOW());

-- 2. Mốc cho Lúa (00000000-0000-0000-000800000004)
INSERT IGNORE INTO cultivation_milestone (
    name, description, activity_type, expected_days_from_planting, product_category_id, standard_id, is_mandatory, created_at
) VALUES 
('Gieo sạ lúa', 'Gieo mạ hoặc sạ lúa xuống ruộng', 'PLANTING', 0, '00000000-0000-0000-000800000004', NULL, TRUE, NOW()),
('Tưới nước dưỡng cây', 'Tưới nước giữ ẩm cho ruộng lúa', 'WATERING', 5, '00000000-0000-0000-000800000004', NULL, TRUE, NOW()),
('Bón phân đợt một', 'Bón phân lót và thúc đợt 1 sau khi gieo', 'FERTILIZING', 10, '00000000-0000-0000-000800000004', NULL, TRUE, NOW()),
('Phun thuốc phòng trừ sâu bệnh', 'Phun thuốc sinh học bảo vệ thực vật', 'PESTICIDE', 25, '00000000-0000-0000-000800000004', NULL, TRUE, NOW());

-- 3. Mốc cho Nho (00000000-0000-0000-000800000001)
INSERT IGNORE INTO cultivation_milestone (
    name, description, activity_type, expected_days_from_planting, product_category_id, standard_id, is_mandatory, created_at
) VALUES 
('Tưới nước gốc nho', 'Tưới nhỏ giọt duy trì độ ẩm rễ nho', 'WATERING', 7, '00000000-0000-0000-000800000001', NULL, TRUE, NOW()),
('Bón phân vi sinh cho nho', 'Bón phân hữu cơ vi sinh bón gốc nho', 'FERTILIZING', 15, '00000000-0000-0000-000800000001', NULL, TRUE, NOW());

-- 4. Mốc cho Xoài (ec98caad-4a6b-410a-99e0-343a42face2a)
INSERT IGNORE INTO cultivation_milestone (
    name, description, activity_type, expected_days_from_planting, product_category_id, standard_id, is_mandatory, created_at
) VALUES 
('Tưới nước vườn xoài', 'Tưới nước định kỳ cho vườn xoài', 'WATERING', 7, 'ec98caad-4a6b-410a-99e0-343a42face2a', NULL, TRUE, NOW()),
('Bón phân thúc cho xoài', 'Bón phân NPK thúc sinh trưởng xoài', 'FERTILIZING', 14, 'ec98caad-4a6b-410a-99e0-343a42face2a', NULL, TRUE, NOW());

-- 5. Mốc cho Sầu riêng (00000000-0000-0000-000800000005)
INSERT IGNORE INTO cultivation_milestone (
    name, description, activity_type, expected_days_from_planting, product_category_id, standard_id, is_mandatory, created_at
) VALUES 
('Tưới nước sầu riêng', 'Tưới nước giữ ẩm đất vùng rễ sầu riêng', 'WATERING', 5, '00000000-0000-0000-000800000005', NULL, TRUE, NOW()),
('Bón phân lá sầu riêng', 'Phun bón phân vi lượng qua lá sầu riêng', 'FERTILIZING', 12, '00000000-0000-0000-000800000005', NULL, TRUE, NOW());

-- 6. Mốc cho Cam (acc9c60c-309e-4fae-813e-3f74fb923dfd)
INSERT IGNORE INTO cultivation_milestone (
    name, description, activity_type, expected_days_from_planting, product_category_id, standard_id, is_mandatory, created_at
) VALUES 
('Tưới nước cam', 'Tưới dưỡng ẩm gốc cam', 'WATERING', 6, 'acc9c60c-309e-4fae-813e-3f74fb923dfd', NULL, TRUE, NOW()),
('Bón phân thúc cho cam', 'Bón phân gốc thúc sinh trưởng cây cam', 'FERTILIZING', 14, 'acc9c60c-309e-4fae-813e-3f74fb923dfd', NULL, TRUE, NOW());

-- 7. Mốc cho Bưởi (00000000-0000-0000-000400000003)
INSERT IGNORE INTO cultivation_milestone (
    name, description, activity_type, expected_days_from_planting, product_category_id, standard_id, is_mandatory, created_at
) VALUES 
('Tưới nước vườn bưởi', 'Tưới định kỳ giữ ẩm vườn bưởi', 'WATERING', 7, '00000000-0000-0000-000400000003', NULL, TRUE, NOW()),
('Bón phân đợt một cho bưởi', 'Bón phân hữu cơ vi sinh gốc bưởi', 'FERTILIZING', 15, '00000000-0000-0000-000400000003', NULL, TRUE, NOW());

-- 8. Mốc cho Chè (a59feb09-820b-44c9-aac0-57b41b1aefb7)
INSERT IGNORE INTO cultivation_milestone (
    name, description, activity_type, expected_days_from_planting, product_category_id, standard_id, is_mandatory, created_at
) VALUES 
('Tưới giữ ẩm đồi chè', 'Tưới phun sương duy trì độ ẩm cho búp chè', 'WATERING', 5, 'a59feb09-820b-44c9-aac0-57b41b1aefb7', NULL, TRUE, NOW()),
('Bón phân lót cho chè', 'Bón phân hữu cơ cho đồi chè', 'FERTILIZING', 10, 'a59feb09-820b-44c9-aac0-57b41b1aefb7', NULL, TRUE, NOW());
