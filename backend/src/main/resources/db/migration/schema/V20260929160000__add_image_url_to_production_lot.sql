-- Thêm cột image_url để lưu ảnh đại diện sản phẩm cho Lô sản xuất
ALTER TABLE production_lot
    ADD COLUMN image_url VARCHAR(500) NULL AFTER approval_notes;
