-- ============================================================
-- V19: Add storage threshold columns to product_categories
-- for NCL-05-CN-007: Theo dõi điều kiện bảo quản khi vận chuyển
-- ============================================================

ALTER TABLE product_categories
    ADD COLUMN temp_min DECIMAL(4,1) NULL,
    ADD COLUMN temp_max DECIMAL(4,1) NULL,
    ADD COLUMN humidity_min DECIMAL(5,1) NULL,
    ADD COLUMN humidity_max DECIMAL(5,1) NULL;