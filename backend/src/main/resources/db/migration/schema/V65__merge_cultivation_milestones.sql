-- ============================================================
-- V65: Merge 2 milestone tables into 1 (cultivation_milestone)
-- Story: NCL-09-CN-011
--
-- Mô tả nghiệp vụ (đã chốt):
--   - 1 bảng mốc duy nhất: tên, mô tả, loại nông sản áp dụng (NULL = toàn bộ),
--     tiêu chuẩn áp dụng (NULL = mọi tiêu chuẩn), bắt buộc, expected days,
--     loại hoạt động (map farm_log.activity_type).
--   - Bỏ 'ngừng sử dụng' (status ACTIVE/INACTIVE) => chỉ còn is_mandatory.
--   - Bỏ xóa mốc (không có DELETE).
--   - Chặn trùng tên trong cùng (product_category_id, standard_id):
--     dùng generated column name_key = LOWER(name) cho unique chống trùng
--     (không nhạy hoa thường) khi cả 2 id không NULL.
-- ============================================================

-- 1. Tạo bảng hợp nhất
CREATE TABLE cultivation_milestone (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    activity_type VARCHAR(30) NOT NULL,
    expected_days_from_planting INT,
    product_category_id CHAR(36),
    standard_id CHAR(36),
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    name_key VARCHAR(150) GENERATED ALWAYS AS (LOWER(name)) STORED,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_milestone_name_cat_std UNIQUE (product_category_id, standard_id, name_key),
    CONSTRAINT fk_milestone_category
        FOREIGN KEY (product_category_id)
        REFERENCES product_categories(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_milestone_standard
        FOREIGN KEY (standard_id)
        REFERENCES standards(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_milestone_name ON cultivation_milestone(name);
CREATE INDEX idx_milestone_activity_type ON cultivation_milestone(activity_type);
CREATE INDEX idx_milestone_category ON cultivation_milestone(product_category_id);
CREATE INDEX idx_milestone_standard ON cultivation_milestone(standard_id);

-- 2. Bỏ 2 bảng cũ (bảng join trước, rồi catalog)
DROP TABLE product_category_milestones;
DROP TABLE cultivation_milestone_catalog;
