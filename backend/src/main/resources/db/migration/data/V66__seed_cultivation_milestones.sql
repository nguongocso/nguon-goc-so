-- ============================================================
-- V66: Seed cultivation_milestone (bảng mốc hợp nhất)
-- Story: NCL-09-CN-011
--
-- Seed lại từ đầu theo mô tả nghiệp vụ đã chốt:
--   - product_category_id NULL  = áp dụng cho TOÀN BỘ loại nông sản
--   - standard_id NULL          = áp dụng cho MỌI tiêu chuẩn
--   - is_mandatory              = BẮT BUỘC (thay 'ngừng sử dụng')
--   - activity_type             = loại hoạt động map với farm_log
-- ============================================================

INSERT INTO cultivation_milestone
    (name, description, activity_type, expected_days_from_planting,
     product_category_id, standard_id, is_mandatory, created_at, updated_at)
VALUES
    -- ===================== Cà phê (loại cụ thể) =====================
    ('Gieo trồng cà phê', 'Tiến hành gieo trồng lô cà phê theo quy trình',
     'PLANTING', 0,
     '00000000-0000-0000-0000-000800000008', NULL, TRUE, NOW(), NOW()),
    ('Bón phân lót cà phê', 'Bón phân lót trước khi gieo hoặc ngay sau gieo',
     'FERTILIZING', 7,
     '00000000-0000-0000-0000-000800000008', NULL, TRUE, NOW(), NOW()),
    ('Phun thuốc phòng sâu cà phê', 'Phun thuốc bảo vệ thực vật phòng ngừa sâu bệnh',
     'PESTICIDE', 21,
     '00000000-0000-0000-0000-000800000008', 'f0892236-d181-4b3c-a4cd-2d394829a877', TRUE, NOW(), NOW()),
    ('Thu hoạch cà phê', 'Thu hoạch cà phê khi đạt yêu cầu chín',
     'HARVESTING', 90,
     '00000000-0000-0000-0000-000800000008', NULL, TRUE, NOW(), NOW()),

    -- ===================== Chè (loại cụ thể) =====================
    ('Gieo trồng chè', 'Tiến hành trồng mới/trồng bù cây chè',
     'PLANTING', 0,
     '00000000-0000-0000-0000-000800000002', NULL, TRUE, NOW(), NOW()),
    ('Bón phân đợt 1 chè', 'Bón phân cho vườn chè theo quy trình',
     'FERTILIZING', 10,
     '00000000-0000-0000-0000-000800000002', NULL, TRUE, NOW(), NOW()),
    ('Phun thuốc phòng ngừa chè', 'Phun thuốc bảo vệ thực vật phòng ngừa rầy xanh',
     'PESTICIDE', 25,
     '00000000-0000-0000-0000-000800000002', 'f0892236-d181-4b3c-a4cd-2d394829a877', FALSE, NOW(), NOW()),
    ('Thu hoạch chè', 'Thu hoạch búp chè khi đạt độ sinh trưởng',
     'HARVESTING', 75,
     '00000000-0000-0000-0000-000800000002', NULL, TRUE, NOW(), NOW()),

    -- ===================== Mốc áp dụng toàn bộ loại (category NULL) =====================
    ('Kiểm tra tưới nước định kỳ', 'Kiểm tra và tưới nước định kỳ cho cây trồng',
     'WATERING', 30, NULL, NULL, FALSE, NOW(), NOW()),
    ('Làm cỏ định kỳ', 'Vệ sinh đồng ruộng, làm cỏ định kỳ',
     'WEEDING', 45, NULL, NULL, FALSE, NOW(), NOW());
