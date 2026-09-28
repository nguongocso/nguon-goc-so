-- ============================================================
-- V20260909100000: Bổ sung cột max_storage_days cho bảng product_categories
-- Hỗ trợ theo dõi thời gian lưu kho tối đa cho phép của danh mục sản phẩm
-- ============================================================

ALTER TABLE product_categories
    ADD COLUMN max_storage_days INT NULL;
