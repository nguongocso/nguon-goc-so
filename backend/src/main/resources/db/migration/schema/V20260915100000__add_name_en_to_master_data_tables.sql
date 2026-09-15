-- ============================================================
-- V20260915100000: Bổ sung trường tên tiếng Anh (name_en) cho các bảng danh mục quản trị
-- User Story: NCL-06-CN-004 - Trang tra cứu công khai bằng tiếng Anh
-- ============================================================

-- 1. Bổ sung trường name_en vào bảng product_categories
ALTER TABLE product_categories
    ADD COLUMN name_en VARCHAR(255) NULL AFTER name;

-- 2. Bổ sung trường name_en vào bảng standards
ALTER TABLE standards
    ADD COLUMN name_en VARCHAR(255) NULL AFTER name;

-- 3. Bổ sung trường name_en vào bảng inspection_criteria
ALTER TABLE inspection_criteria
    ADD COLUMN name_en VARCHAR(255) NULL AFTER criterion_name;

-- 4. Bổ sung trường name_en vào bảng inspection_criterion_catalog
ALTER TABLE inspection_criterion_catalog
    ADD COLUMN name_en VARCHAR(150) NULL AFTER name;
