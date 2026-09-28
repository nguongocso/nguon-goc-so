-- ============================================================
-- V20260907160001: Seed core entities for testing NCL-04-CN-008
-- Phase 2: Organization, Roles, Users, OrganizationUsers,
--          ProductCategory, FarmArea, InputMaterial
--
-- Đặc điểm: Idempotent (INSERT IGNORE + Deterministic UUIDs + Session variables)
-- ============================================================

-- ------------------------------------------------------------
-- 1. Organization: "HTX Nông sản Xanh" (code: HTX_TEST)
-- ------------------------------------------------------------
INSERT IGNORE INTO organizations (
    organization_id,
    name,
    code,
    type,
    status,
    address,
    phone,
    email,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0000-000100000001',
    'HTX Nông sản Xanh',
    'HTX_TEST',
    'COOPERATIVE',
    'ACTIVE',
    'Xã Tân Phú, Huyện Châu Thành, Tỉnh Bến Tre',
    '0901234567',
    'htx.nongsanxanh@test.nguongocso.vn',
    NOW(),
    NOW()
);

-- Lưu biến session cho organization_id
SET @htx_org_id = (SELECT organization_id FROM organizations WHERE code = 'HTX_TEST' LIMIT 1);

-- ------------------------------------------------------------
-- 2. Roles: VT-01 (ADMIN), VT-02 (ORG_MANAGER), VT-03 (EVENT_RECORDER)
-- ------------------------------------------------------------
INSERT IGNORE INTO roles (code, name) VALUES
    ('VT-01', 'ADMIN'),
    ('VT-02', 'ORG_MANAGER'),
    ('VT-03', 'EVENT_RECORDER');

-- ------------------------------------------------------------
-- 3. Users:
--    - quanly_htx (VT-02, Nguyễn Văn Quản Lý)
--    - nguoighi (VT-03, Trần Thị Ghi Chép)
--    - admin (VT-01, Quản Trị Viên)
-- Password mặc định: admin123 (hash bcrypt)
-- ------------------------------------------------------------
INSERT IGNORE INTO users (
    user_id,
    user_name,
    password_hash,
    full_name,
    phone,
    email,
    avatar_url,
    status,
    created_at,
    updated_at
) VALUES
(
    '00000000-0000-0000-0000-000200000001',
    'quanly_htx',
    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
    'Nguyễn Văn Quản Lý',
    '0912000001',
    'quanly_htx@test.nguongocso.vn',
    NULL,
    'ACTIVE',
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000200000002',
    'nguoighi',
    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
    'Trần Thị Ghi Chép',
    '0912000002',
    'nguoighi@test.nguongocso.vn',
    NULL,
    'ACTIVE',
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000200000003',
    'admin',
    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
    'Quản Trị Viên',
    '0912000000',
    'admin@test.nguongocso.vn',
    NULL,
    'ACTIVE',
    NOW(),
    NOW()
);

-- Lưu biến session cho users
SET @user_manager_id = (SELECT user_id FROM users WHERE user_name = 'quanly_htx' LIMIT 1);
SET @user_recorder_id = (SELECT user_id FROM users WHERE user_name = 'nguoighi' LIMIT 1);
SET @user_admin_id = (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1);

-- ------------------------------------------------------------
-- 4. OrganizationUsers: Gán quyền cho users vào HTX_TEST
-- ------------------------------------------------------------
-- Gán quanly_htx vai trò VT-02 trong HTX_TEST
INSERT IGNORE INTO organization_users (
    id,
    organization_id,
    user_id,
    role_id,
    custom_permissions,
    joined_at,
    status
)
SELECT
    '00000000-0000-0000-0000-000300000001',
    @htx_org_id,
    @user_manager_id,
    r.role_id,
    NULL,
    NOW(),
    'ACTIVE'
FROM roles r
WHERE r.code = 'VT-02'
  AND @htx_org_id IS NOT NULL
  AND @user_manager_id IS NOT NULL;

-- Gán nguoighi vai trò VT-03 trong HTX_TEST
INSERT IGNORE INTO organization_users (
    id,
    organization_id,
    user_id,
    role_id,
    custom_permissions,
    joined_at,
    status
)
SELECT
    '00000000-0000-0000-0000-000300000002',
    @htx_org_id,
    @user_recorder_id,
    r.role_id,
    NULL,
    NOW(),
    'ACTIVE'
FROM roles r
WHERE r.code = 'VT-03'
  AND @htx_org_id IS NOT NULL
  AND @user_recorder_id IS NOT NULL;

-- Gán admin vai trò VT-01 trong HTX_TEST
INSERT IGNORE INTO organization_users (
    id,
    organization_id,
    user_id,
    role_id,
    custom_permissions,
    joined_at,
    status
)
SELECT
    '00000000-0000-0000-0000-000300000003',
    @htx_org_id,
    @user_admin_id,
    r.role_id,
    NULL,
    NOW(),
    'ACTIVE'
FROM roles r
WHERE r.code = 'VT-01'
  AND @htx_org_id IS NOT NULL
  AND @user_admin_id IS NOT NULL;

-- ------------------------------------------------------------
-- 5. ProductCategory: "Xoài", "Cam", "Bưởi"
-- ------------------------------------------------------------
INSERT IGNORE INTO product_categories (
    id,
    name,
    category_group,
    description,
    is_active,
    requires_inspection
) VALUES
(
    '00000000-0000-0000-0000-000400000001',
    'Xoài',
    'Cây ăn quả',
    'Xoài cát Chu đặc sản Bến Tre',
    TRUE,
    FALSE
),
(
    '00000000-0000-0000-0000-000400000002',
    'Cam',
    'Cây ăn quả',
    'Cam sành Bến Tre mọng nước',
    TRUE,
    FALSE
),
(
    '00000000-0000-0000-0000-000400000003',
    'Bưởi',
    'Cây ăn quả',
    'Bưởi da xanh ruột hồng Bến Tre',
    TRUE,
    FALSE
);

-- Lưu biến session cho product_categories (truy vấn theo tên để tương thích nếu đã có sẵn)
SET @cat_xoai_id = (SELECT id FROM product_categories WHERE name = 'Xoài' LIMIT 1);
SET @cat_cam_id = (SELECT id FROM product_categories WHERE name = 'Cam' LIMIT 1);
SET @cat_buoi_id = (SELECT id FROM product_categories WHERE name = 'Bưởi' LIMIT 1);

-- ------------------------------------------------------------
-- 6. FarmArea: "Vùng trồng A" (Xoài), "Vùng trồng B" (Cam), "Vùng trồng C" (Bưởi)
-- ------------------------------------------------------------
INSERT IGNORE INTO farm_areas (
    id,
    organization_id,
    crop_type,
    name,
    area,
    area_unit,
    location,
    is_active,
    created_at,
    updated_at
) VALUES
(
    '00000000-0000-0000-0000-000500000001',
    @htx_org_id,
    @cat_xoai_id,
    'Vùng trồng A',
    3.50,
    'HA',
    ST_GeomFromText('POINT(106.3753 10.2433)'),
    TRUE,
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000500000002',
    @htx_org_id,
    @cat_cam_id,
    'Vùng trồng B',
    2.80,
    'HA',
    ST_GeomFromText('POINT(106.3812 10.2485)'),
    TRUE,
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000500000003',
    @htx_org_id,
    @cat_buoi_id,
    'Vùng trồng C',
    4.20,
    'HA',
    ST_GeomFromText('POINT(106.3905 10.2521)'),
    TRUE,
    NOW(),
    NOW()
);

-- Lưu biến session cho farm_areas
SET @area_a_id = (SELECT id FROM farm_areas WHERE name = 'Vùng trồng A' AND organization_id = @htx_org_id LIMIT 1);
SET @area_b_id = (SELECT id FROM farm_areas WHERE name = 'Vùng trồng B' AND organization_id = @htx_org_id LIMIT 1);
SET @area_c_id = (SELECT id FROM farm_areas WHERE name = 'Vùng trồng C' AND organization_id = @htx_org_id LIMIT 1);

-- ------------------------------------------------------------
-- 7. InputMaterial: 5 loại vật tư nông nghiệp
-- ------------------------------------------------------------
INSERT IGNORE INTO input_materials (
    id,
    name,
    material_group,
    active_ingredient,
    unit,
    quarantine_days,
    apply_to_all_crops,
    reference_source,
    image_urls,
    is_active,
    created_by,
    created_at,
    updated_at
) VALUES
(
    '00000000-0000-0000-0000-000600000001',
    'NPK 16-16-8',
    'FERTILIZER',
    'N-P-K (16-16-8)',
    'kg',
    0,
    TRUE,
    'Danh mục phân bón lưu hành tại Việt Nam',
    NULL,
    TRUE,
    @user_manager_id,
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000600000002',
    'Thuốc trừ sâu ABC',
    'PESTICIDE',
    'Abamectin 3.6EC',
    'lít',
    7,
    TRUE,
    'Danh mục thuốc BVTV được phép sử dụng tại Việt Nam',
    NULL,
    TRUE,
    @user_manager_id,
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000600000003',
    'Thuốc trừ sâu XYZ',
    'PESTICIDE',
    'Emamectin benzoate 5WG',
    'gói',
    14,
    TRUE,
    'Danh mục thuốc BVTV được phép sử dụng tại Việt Nam',
    NULL,
    TRUE,
    @user_manager_id,
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000600000004',
    'Phân hữu cơ',
    'FERTILIZER',
    'Hữu cơ vi sinh 65%',
    'kg',
    0,
    TRUE,
    'Danh mục phân bón lưu hành tại Việt Nam',
    NULL,
    TRUE,
    @user_manager_id,
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000600000005',
    'Chế phẩm sinh học',
    'BIOLOGICAL',
    'Bacillus subtilis',
    'lít',
    0,
    TRUE,
    'Quy trình quản lý dịch hại tổng hợp IPM',
    NULL,
    TRUE,
    @user_manager_id,
    NOW(),
    NOW()
);

-- Liên kết vật tư với cây trồng trong bảng input_material_crop_types
INSERT IGNORE INTO input_material_crop_types (material_id, crop_category_id)
SELECT m.id, c.id
FROM input_materials m
CROSS JOIN product_categories c
WHERE m.id IN (
    '00000000-0000-0000-0000-000600000001',
    '00000000-0000-0000-0000-000600000002',
    '00000000-0000-0000-0000-000600000003',
    '00000000-0000-0000-0000-000600000004',
    '00000000-0000-0000-0000-000600000005'
)
AND c.id IN (@cat_xoai_id, @cat_cam_id, @cat_buoi_id);

-- Lưu biến session cho input_materials
SET @mat_npk_id = (SELECT id FROM input_materials WHERE name = 'NPK 16-16-8' LIMIT 1);
SET @mat_pest_abc_id = (SELECT id FROM input_materials WHERE name = 'Thuốc trừ sâu ABC' LIMIT 1);
SET @mat_pest_xyz_id = (SELECT id FROM input_materials WHERE name = 'Thuốc trừ sâu XYZ' LIMIT 1);
SET @mat_organic_id = (SELECT id FROM input_materials WHERE name = 'Phân hữu cơ' LIMIT 1);
SET @mat_bio_id = (SELECT id FROM input_materials WHERE name = 'Chế phẩm sinh học' LIMIT 1);