-- ============================================================
-- V35: Administrative units & user area assignments
--      (NCL-670/NCL-740 - data layer cho gán địa bàn quản lý VT-05)
-- Depends on: users, organizations
-- ============================================================

-- Danh mục đơn vị hành chính dùng chung (mô hình 2 cấp hiệu lực 01/07/2025:
-- 34 tỉnh/thành phố trực tiếp quản lý xã/phường/đặc khu). Cây tự tham chiếu qua
-- parent_id; province_id denormalize trỏ về gốc cấp tỉnh để lọc báo cáo O(1).
CREATE TABLE administrative_units (
    id CHAR(36) NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    level VARCHAR(20) NOT NULL,
    parent_id CHAR(36) NULL,
    province_id CHAR(36) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_administrative_units PRIMARY KEY (id),
    CONSTRAINT uk_administrative_unit_code UNIQUE (code),
    CONSTRAINT fk_admin_unit_parent FOREIGN KEY (parent_id) REFERENCES administrative_units (id),
    CONSTRAINT fk_admin_unit_province FOREIGN KEY (province_id) REFERENCES administrative_units (id)
) ENGINE=InnoDB;

CREATE INDEX idx_admin_unit_parent ON administrative_units (parent_id);
CREATE INDEX idx_admin_unit_province ON administrative_units (province_id);
CREATE INDEX idx_admin_unit_level ON administrative_units (level);

-- Quan hệ nhiều-nhiều tài khoản <-> địa bàn phụ trách.
-- UNIQUE (user_id, unit_id) chặn gán trùng ngay ở tầng dữ liệu (chống race-condition).
CREATE TABLE user_area_assignments (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    unit_id CHAR(36) NOT NULL,
    assigned_at DATETIME(6) NOT NULL,
    assigned_by CHAR(36) NULL,
    CONSTRAINT pk_user_area_assignments PRIMARY KEY (id),
    CONSTRAINT fk_user_area_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_user_area_unit FOREIGN KEY (unit_id) REFERENCES administrative_units (id),
    CONSTRAINT fk_user_area_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (user_id),
    CONSTRAINT uk_user_area_assignment UNIQUE (user_id, unit_id)
) ENGINE=InnoDB;

CREATE INDEX idx_user_area_user ON user_area_assignments (user_id);
CREATE INDEX idx_user_area_unit ON user_area_assignments (unit_id);

-- Map địa bàn ở mức tổ chức (nullable): đầu vào cho bộ lọc báo cáo của VT-05.
ALTER TABLE organizations
    ADD COLUMN province_id CHAR(36) NULL,
    ADD COLUMN commune_id CHAR(36) NULL;

ALTER TABLE organizations
    ADD CONSTRAINT fk_org_province FOREIGN KEY (province_id) REFERENCES administrative_units (id),
    ADD CONSTRAINT fk_org_commune FOREIGN KEY (commune_id) REFERENCES administrative_units (id);
