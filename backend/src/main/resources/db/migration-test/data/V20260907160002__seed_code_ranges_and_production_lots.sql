-- ============================================================
-- V20260907160002: Seed code ranges and production lots
-- Phase 3: CodeRange (2 dải mã) & ProductionLot (5 lô sản xuất)
--
-- Đặc điểm: Idempotent (INSERT IGNORE + Deterministic UUIDs + Session variables)
-- ============================================================

-- ------------------------------------------------------------
-- 0. Khởi tạo/Nạp lại các biến session SQL từ Phase 2
-- ------------------------------------------------------------
SET @htx_org_id = (SELECT organization_id FROM organizations WHERE code = 'HTX_TEST' LIMIT 1);
SET @user_manager_id = (SELECT user_id FROM users WHERE user_name = 'quanly_htx' LIMIT 1);

SET @cat_xoai_id = (SELECT id FROM product_categories WHERE name = 'Xoài' LIMIT 1);
SET @cat_cam_id = (SELECT id FROM product_categories WHERE name = 'Cam' LIMIT 1);
SET @cat_buoi_id = (SELECT id FROM product_categories WHERE name = 'Bưởi' LIMIT 1);

SET @area_a_id = (SELECT id FROM farm_areas WHERE name = 'Vùng trồng A' AND organization_id = @htx_org_id LIMIT 1);
SET @area_b_id = (SELECT id FROM farm_areas WHERE name = 'Vùng trồng B' AND organization_id = @htx_org_id LIMIT 1);
SET @area_c_id = (SELECT id FROM farm_areas WHERE name = 'Vùng trồng C' AND organization_id = @htx_org_id LIMIT 1);

-- ------------------------------------------------------------
-- 1. CodeRange: 2 dải mã truy xuất thuộc HTX_TEST
--    - 893001: limit 1000, used 350
--    - 893002: limit 500, used 120
-- ------------------------------------------------------------
INSERT IGNORE INTO code_ranges (
    id,
    organization_id,
    prefix,
    from_number,
    to_number,
    total_limit,
    used_count,
    created_by,
    created_at,
    updated_at
) VALUES
(
    '00000000-0000-0000-0000-000700000001',
    @htx_org_id,
    '893001',
    1,
    1000,
    1000,
    350,
    @user_manager_id,
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000700000002',
    @htx_org_id,
    '893002',
    1,
    500,
    500,
    120,
    @user_manager_id,
    NOW(),
    NOW()
);

-- Lưu biến session cho code_ranges
SET @range_893001_id = (SELECT id FROM code_ranges WHERE prefix = '893001' LIMIT 1);
SET @range_893002_id = (SELECT id FROM code_ranges WHERE prefix = '893002' LIMIT 1);

-- ------------------------------------------------------------
-- 2. ProductionLot: 5 lô sản xuất với các trạng thái khác nhau
--    - Lot 1: "Lô Xoài Xuân 2026"  - Xoài  - APPROVED
--    - Lot 2: "Lô Cam Hạ 2026"     - Cam   - APPROVED
--    - Lot 3: "Lô Bưởi Thu 2026"   - Bưởi  - HARVESTED
--    - Lot 4: "Lô Xoài Hè 2026"    - Xoài  - PACKAGED
--    - Lot 5: "Lô Cam Đông 2026"   - Cam   - RECALLED
-- ------------------------------------------------------------
INSERT IGNORE INTO production_lot (
    id,
    organization_id,
    farm_area_id,
    product_category_id,
    name,
    expected_quantity,
    expected_quantity_unit,
    actual_quantity,
    planting_date,
    harvest_date,
    status,
    approval_notes,
    created_by,
    approved_by,
    created_at,
    updated_at
) VALUES
(
    '00000000-0000-0000-0000-000800000001',
    @htx_org_id,
    @area_a_id,
    @cat_xoai_id,
    'Lô Xoài Xuân 2026',
    5000,
    'kg',
    NULL,
    '2026-01-15',
    NULL,
    'APPROVED',
    'Đã thẩm định nhật ký canh tác đầu vụ, đủ điều kiện sinh trưởng tốt',
    @user_manager_id,
    @user_manager_id,
    '2026-01-15 08:00:00',
    '2026-01-16 09:30:00'
),
(
    '00000000-0000-0000-0000-000800000002',
    @htx_org_id,
    @area_b_id,
    @cat_cam_id,
    'Lô Cam Hạ 2026',
    3000,
    'kg',
    NULL,
    '2026-02-01',
    NULL,
    'APPROVED',
    'Đạt tiêu chuẩn quy trình VietGAP, đã duyệt kế hoạch chăm sóc',
    @user_manager_id,
    @user_manager_id,
    '2026-02-01 08:30:00',
    '2026-02-02 10:00:00'
),
(
    '00000000-0000-0000-0000-000800000003',
    @htx_org_id,
    @area_c_id,
    @cat_buoi_id,
    'Lô Bưởi Thu 2026',
    4000,
    'kg',
    3800,
    '2026-01-10',
    '2026-07-20',
    'HARVESTED',
    'Đã thu hoạch thành công, mẫu quả đồng đều đạt chuẩn xuất khẩu',
    @user_manager_id,
    @user_manager_id,
    '2026-01-10 07:30:00',
    '2026-07-20 16:00:00'
),
(
    '00000000-0000-0000-0000-000800000004',
    @htx_org_id,
    @area_a_id,
    @cat_xoai_id,
    'Lô Xoài Hè 2026',
    6000,
    'kg',
    5500,
    '2026-02-20',
    '2026-08-01',
    'PACKAGED',
    'Đã sơ chế phân loại và đóng gói hoàn tất, sẵn sàng xuất kho',
    @user_manager_id,
    @user_manager_id,
    '2026-02-20 08:00:00',
    '2026-08-05 14:30:00'
),
(
    '00000000-0000-0000-0000-000800000005',
    @htx_org_id,
    @area_b_id,
    @cat_cam_id,
    'Lô Cam Đông 2026',
    2500,
    'kg',
    2200,
    '2026-01-05',
    '2026-06-15',
    'RECALLED',
    'Lô hàng bị thu hồi theo quyết định kiểm định an toàn thực phẩm',
    @user_manager_id,
    @user_manager_id,
    '2026-01-05 08:00:00',
    '2026-07-01 11:00:00'
);

-- Lưu biến session cho production_lot phục vụ Phase 4 & 5
SET @lot_xoai_xuan_id = (SELECT id FROM production_lot WHERE name = 'Lô Xoài Xuân 2026' LIMIT 1);
SET @lot_cam_ha_id    = (SELECT id FROM production_lot WHERE name = 'Lô Cam Hạ 2026' LIMIT 1);
SET @lot_buoi_thu_id  = (SELECT id FROM production_lot WHERE name = 'Lô Bưởi Thu 2026' LIMIT 1);
SET @lot_xoai_he_id   = (SELECT id FROM production_lot WHERE name = 'Lô Xoài Hè 2026' LIMIT 1);
SET @lot_cam_dong_id  = (SELECT id FROM production_lot WHERE name = 'Lô Cam Đông 2026' LIMIT 1);
