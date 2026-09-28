-- ============================================================
-- V20260907160003: Seed farm logs and chain events
-- Phase 4: FarmLog (18 nhật ký) & ChainEvent (5 sự kiện: HARVEST & PACKAGING)
--
-- Đặc điểm: Idempotent (INSERT IGNORE + Deterministic UUIDs + Session variables)
-- ============================================================

-- ------------------------------------------------------------
-- 0. Nạp lại các biến session SQL cần thiết
-- ------------------------------------------------------------
SET @htx_org_id = (SELECT organization_id FROM organizations WHERE code = 'HTX_TEST' LIMIT 1);
SET @user_recorder_id = (SELECT user_id FROM users WHERE user_name = 'nguoighi' LIMIT 1);

SET @lot_xoai_xuan_id = (SELECT id FROM production_lot WHERE name = 'Lô Xoài Xuân 2026' LIMIT 1);
SET @lot_cam_ha_id    = (SELECT id FROM production_lot WHERE name = 'Lô Cam Hạ 2026' LIMIT 1);
SET @lot_buoi_thu_id  = (SELECT id FROM production_lot WHERE name = 'Lô Bưởi Thu 2026' LIMIT 1);
SET @lot_xoai_he_id   = (SELECT id FROM production_lot WHERE name = 'Lô Xoài Hè 2026' LIMIT 1);
SET @lot_cam_dong_id  = (SELECT id FROM production_lot WHERE name = 'Lô Cam Đông 2026' LIMIT 1);

-- ------------------------------------------------------------
-- 1. FarmLog: 18 nhật ký canh tác cho 5 lô sản xuất
-- ------------------------------------------------------------

-- A. Lô Xoài Xuân 2026 (APPROVED): 3 nhật ký
INSERT IGNORE INTO farm_logs (
    id, production_lot_id, activity_type, material, quantity, unit, executed_date, notes, created_by, created_at, is_correction, is_corrected
) VALUES
(
    '00000000-0000-0000-0000-000900000001',
    @lot_xoai_xuan_id,
    'PLANTING',
    'Cây giống Xoài cát Chu',
    200,
    'cây',
    '2026-01-15',
    'Xuống giống theo đúng kỹ thuật khoảng cách 6m x 6m',
    @user_recorder_id,
    '2026-01-15 09:00:00',
    FALSE,
    FALSE
),
(
    '00000000-0000-0000-0000-000900000002',
    @lot_xoai_xuan_id,
    'FERTILIZING',
    'Phân hữu cơ',
    150,
    'kg',
    '2026-02-01',
    'Bón lót phân hữu cơ vi sinh quanh gốc kích thích ra rễ',
    @user_recorder_id,
    '2026-02-01 08:30:00',
    FALSE,
    FALSE
),
(
    '00000000-0000-0000-0000-000900000003',
    @lot_xoai_xuan_id,
    'PESTICIDE',
    'Chế phẩm sinh học',
    10,
    'lít',
    '2026-03-01',
    'Phun chế phẩm sinh học phòng ngừa bọ trĩ và rệp sáp đầu mùa',
    @user_recorder_id,
    '2026-03-01 10:00:00',
    FALSE,
    FALSE
);

-- B. Lô Cam Hạ 2026 (APPROVED): 3 nhật ký
INSERT IGNORE INTO farm_logs (
    id, production_lot_id, activity_type, material, quantity, unit, executed_date, notes, created_by, created_at, is_correction, is_corrected
) VALUES
(
    '00000000-0000-0000-0000-000900000004',
    @lot_cam_ha_id,
    'PLANTING',
    'Cây giống Cam sành',
    300,
    'cây',
    '2026-02-01',
    'Trồng cây con ghép tuyển chọn, xử lý nấm trước khi trồng',
    @user_recorder_id,
    '2026-02-01 09:15:00',
    FALSE,
    FALSE
),
(
    '00000000-0000-0000-0000-000900000005',
    @lot_cam_ha_id,
    'FERTILIZING',
    'NPK 16-16-8',
    120,
    'kg',
    '2026-03-01',
    'Bón thúc đợt 1 bổ sung đạm và lân cho cơi đọt mới',
    @user_recorder_id,
    '2026-03-01 08:45:00',
    FALSE,
    FALSE
),
(
    '00000000-0000-0000-0000-000900000006',
    @lot_cam_ha_id,
    'WEEDING',
    NULL,
    NULL,
    NULL,
    '2026-04-01',
    'Làm sạch cỏ dại quanh tán cây và phát quang bờ rãnh thoát nước',
    @user_recorder_id,
    '2026-04-01 11:00:00',
    FALSE,
    FALSE
);

-- C. Lô Bưởi Thu 2026 (HARVESTED): 4 nhật ký
INSERT IGNORE INTO farm_logs (
    id, production_lot_id, activity_type, material, quantity, unit, executed_date, notes, created_by, created_at, is_correction, is_corrected
) VALUES
(
    '00000000-0000-0000-0000-000900000007',
    @lot_buoi_thu_id,
    'PLANTING',
    'Cây giống Bưởi da xanh',
    250,
    'cây',
    '2026-01-10',
    'Xuống giống bưởi da xanh ruột hồng tại vùng trồng C',
    @user_recorder_id,
    '2026-01-10 08:00:00',
    FALSE,
    FALSE
),
(
    '00000000-0000-0000-0000-000900000008',
    @lot_buoi_thu_id,
    'FERTILIZING',
    'Phân hữu cơ',
    200,
    'kg',
    '2026-02-15',
    'Bón phân hữu cơ vi sinh định kỳ cung cấp dinh dưỡng bền vững',
    @user_recorder_id,
    '2026-02-15 09:30:00',
    FALSE,
    FALSE
),
(
    '00000000-0000-0000-0000-000900000009',
    @lot_buoi_thu_id,
    'PESTICIDE',
    'Thuốc trừ sâu ABC',
    5,
    'lít',
    '2026-03-20',
    'Phun trừ sâu vẽ bùa và nhện đỏ theo đúng nồng độ quy định',
    @user_recorder_id,
    '2026-03-20 16:00:00',
    FALSE,
    FALSE
),
(
    '00000000-0000-0000-0000-000900000010',
    @lot_buoi_thu_id,
    'HARVESTING',
    'Bưởi da xanh loại 1',
    3800,
    'kg',
    '2026-07-20',
    'Thu hoạch trái đạt độ chín chuẩn, phân loại và chuẩn bị đóng sọt',
    @user_recorder_id,
    '2026-07-20 15:30:00',
    FALSE,
    FALSE
);

-- D. Lô Xoài Hè 2026 (PACKAGED): 4 nhật ký
INSERT IGNORE INTO farm_logs (
    id, production_lot_id, activity_type, material, quantity, unit, executed_date, notes, created_by, created_at, is_correction, is_corrected
) VALUES
(
    '00000000-0000-0000-0000-000900000011',
    @lot_xoai_he_id,
    'PLANTING',
    'Cây giống Xoài cát Chu',
    350,
    'cây',
    '2026-02-20',
    'Trồng mới bổ sung vụ hè tại vùng trồng A',
    @user_recorder_id,
    '2026-02-20 08:30:00',
    FALSE,
    FALSE
),
(
    '00000000-0000-0000-0000-000900000012',
    @lot_xoai_he_id,
    'FERTILIZING',
    'NPK 16-16-8',
    250,
    'kg',
    '2026-03-20',
    'Bón phân NPK giai đoạn phát triển hoa và quả non',
    @user_recorder_id,
    '2026-03-20 09:00:00',
    FALSE,
    FALSE
),
(
    '00000000-0000-0000-0000-000900000013',
    @lot_xoai_he_id,
    'PESTICIDE',
    'Chế phẩm sinh học',
    15,
    'lít',
    '2026-04-15',
    'Phun chế phẩm sinh học phòng ngừa bệnh thán thư trước mùa mưa',
    @user_recorder_id,
    '2026-04-15 10:15:00',
    FALSE,
    FALSE
),
(
    '00000000-0000-0000-0000-000900000014',
    @lot_xoai_he_id,
    'HARVESTING',
    'Xoài cát Chu chín cây',
    5500,
    'kg',
    '2026-08-01',
    'Thu hoạch rộ toàn bộ diện tích, sản lượng đạt yêu cầu chất lượng',
    @user_recorder_id,
    '2026-08-01 14:00:00',
    FALSE,
    FALSE
);

-- E. Lô Cam Đông 2026 (RECALLED): 4 nhật ký
INSERT IGNORE INTO farm_logs (
    id, production_lot_id, activity_type, material, quantity, unit, executed_date, notes, created_by, created_at, is_correction, is_corrected
) VALUES
(
    '00000000-0000-0000-0000-000900000015',
    @lot_cam_dong_id,
    'PLANTING',
    'Cây giống Cam sành',
    220,
    'cây',
    '2026-01-05',
    'Xuống giống vụ đông tại vùng trồng B',
    @user_recorder_id,
    '2026-01-05 08:30:00',
    FALSE,
    FALSE
),
(
    '00000000-0000-0000-0000-000900000016',
    @lot_cam_dong_id,
    'FERTILIZING',
    'NPK 16-16-8',
    100,
    'kg',
    '2026-02-10',
    'Bón lót phân khoáng đợt 1',
    @user_recorder_id,
    '2026-02-10 09:00:00',
    FALSE,
    FALSE
),
(
    '00000000-0000-0000-0000-000900000017',
    @lot_cam_dong_id,
    'PESTICIDE',
    'Thuốc trừ sâu XYZ',
    8,
    'gói',
    '2026-03-15',
    'Phun xử lý rệp muội (chú ý thời gian cách ly 14 ngày)',
    @user_recorder_id,
    '2026-03-15 15:00:00',
    FALSE,
    FALSE
),
(
    '00000000-0000-0000-0000-000900000018',
    @lot_cam_dong_id,
    'HARVESTING',
    'Cam sành thu hoạch đợt 1',
    2200,
    'kg',
    '2026-06-15',
    'Thu hoạch cam sành (sau đó phát hiện nghi vấn dư lượng dẫn đến thu hồi)',
    @user_recorder_id,
    '2026-06-15 11:30:00',
    FALSE,
    FALSE
);

-- ------------------------------------------------------------
-- 2. ChainEvent (HARVEST): Sự kiện thu hoạch cho 3 lô đã thu hoạch
-- ------------------------------------------------------------
-- A. Lô Bưởi Thu 2026 (harvest_date = 2026-07-20)
INSERT IGNORE INTO chain_events (
    id, shipment_id, event_type, event_data, location, recorded_at, recorded_by, created_at, is_correction
) VALUES (
    '00000000-0000-0000-0000-000a00000001',
    NULL,
    'HARVEST',
    JSON_OBJECT(
        'productionLotId', @lot_buoi_thu_id,
        'harvestDate', '2026-07-20',
        'quantity', 3800,
        'unit', 'kg',
        'notes', 'Thu hoạch bưởi da xanh đạt chuẩn chất lượng xuất khẩu'
    ),
    ST_GeomFromText('POINT(106.3905 10.2521)'),
    '2026-07-20 16:30:00',
    @user_recorder_id,
    '2026-07-20 16:30:00',
    FALSE
);

-- B. Lô Xoài Hè 2026 (harvest_date = 2026-08-01)
INSERT IGNORE INTO chain_events (
    id, shipment_id, event_type, event_data, location, recorded_at, recorded_by, created_at, is_correction
) VALUES (
    '00000000-0000-0000-0000-000a00000002',
    NULL,
    'HARVEST',
    JSON_OBJECT(
        'productionLotId', @lot_xoai_he_id,
        'harvestDate', '2026-08-01',
        'quantity', 5500,
        'unit', 'kg',
        'notes', 'Thu hoạch xoài cát Chu phân loại loại 1 tại vườn'
    ),
    ST_GeomFromText('POINT(106.3753 10.2433)'),
    '2026-08-01 15:00:00',
    @user_recorder_id,
    '2026-08-01 15:00:00',
    FALSE
);

-- C. Lô Cam Đông 2026 (harvest_date = 2026-06-15)
INSERT IGNORE INTO chain_events (
    id, shipment_id, event_type, event_data, location, recorded_at, recorded_by, created_at, is_correction
) VALUES (
    '00000000-0000-0000-0000-000a00000003',
    NULL,
    'HARVEST',
    JSON_OBJECT(
        'productionLotId', @lot_cam_dong_id,
        'harvestDate', '2026-06-15',
        'quantity', 2200,
        'unit', 'kg',
        'notes', 'Thu hoạch cam sành phân loại chuẩn bị sơ chế'
    ),
    ST_GeomFromText('POINT(106.3812 10.2485)'),
    '2026-06-15 14:00:00',
    @user_recorder_id,
    '2026-06-15 14:00:00',
    FALSE
);

-- ------------------------------------------------------------
-- 3. ChainEvent (PACKAGING): Sự kiện đóng gói cho 2 lô
-- ------------------------------------------------------------
-- A. Lô Xoài Hè 2026 (packaging_date = 2026-08-05)
INSERT IGNORE INTO chain_events (
    id, shipment_id, event_type, event_data, location, recorded_at, recorded_by, created_at, is_correction
) VALUES (
    '00000000-0000-0000-0000-000a00000004',
    NULL,
    'PACKAGING',
    JSON_OBJECT(
        'productionLotId', @lot_xoai_he_id,
        'packagingDate', '2026-08-05',
        'quantity', 5500,
        'packagingType', 'Thùng carton 10kg',
        'notes', 'Đóng gói và dán nhãn truy xuất nguồn gốc QR'
    ),
    ST_GeomFromText('POINT(106.3753 10.2433)'),
    '2026-08-05 16:00:00',
    @user_recorder_id,
    '2026-08-05 16:00:00',
    FALSE
);

-- B. Lô Cam Đông 2026 (packaging_date = 2026-06-20)
INSERT IGNORE INTO chain_events (
    id, shipment_id, event_type, event_data, location, recorded_at, recorded_by, created_at, is_correction
) VALUES (
    '00000000-0000-0000-0000-000a00000005',
    NULL,
    'PACKAGING',
    JSON_OBJECT(
        'productionLotId', @lot_cam_dong_id,
        'packagingDate', '2026-06-20',
        'quantity', 2200,
        'packagingType', 'Sọt nhựa 20kg',
        'notes', 'Đóng gói bảo quản trước khi có quyết định thu hồi'
    ),
    ST_GeomFromText('POINT(106.3812 10.2485)'),
    '2026-06-20 15:30:00',
    @user_recorder_id,
    '2026-06-20 15:30:00',
    FALSE
);
