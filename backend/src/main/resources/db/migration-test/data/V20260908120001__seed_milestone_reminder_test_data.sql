-- ============================================================
-- V20260908120000: Seed mốc canh tác và phân công lô cho test/UI
--                  (User Story: NCL-03-CN-007)
-- ============================================================

-- 1. Seed mốc canh tác bắt buộc cho loại nông sản Lúa (00000000-0000-0000-0000-000800000004)
INSERT IGNORE INTO cultivation_milestone (
    name, description, activity_type, expected_days_from_planting, product_category_id, standard_id, is_mandatory, created_at
) VALUES 
(
    'Bón phân đợt một',
    'Bón phân lót và thúc đợt 1 sau khi gieo sạ',
    'FERTILIZING',
    10,
    '00000000-0000-0000-0000-000800000004',
    NULL,
    TRUE,
    NOW()
),
(
    'Tưới nước dưỡng cây',
    'Tưới nước giữ ẩm cho ruộng lúa trong giai đoạn đẻ nhánh',
    'WATERING',
    5,
    '00000000-0000-0000-0000-000800000004',
    NULL,
    TRUE,
    NOW()
),
(
    'Phun thuốc phòng trừ sâu bệnh',
    'Phun thuốc sinh học bảo vệ thực vật theo tiêu chuẩn',
    'PESTICIDE',
    25,
    '00000000-0000-0000-0000-000800000004',
    NULL,
    TRUE,
    NOW()
);

-- 2. Gán phân công lô Lúa 12 cho tài khoản Người ghi sự kiện (eventrecorder / VT-03)
INSERT IGNORE INTO lot_assignments (
    id, lot_id, user_id, organization_id, active, assigned_at
) VALUES (
    'f1a2b3c4-d5e6-7890-abcd-111122223333',
    '00000000-0000-0000-0000-000200000012',
    'cc599901-a3be-11f1-9ca5-e00af63e88f4',
    'cc57f586-a3be-11f1-9ca5-e00af63e88f4',
    TRUE,
    NOW()
);
