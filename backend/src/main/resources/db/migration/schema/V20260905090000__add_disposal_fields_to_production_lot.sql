-- ============================================================
-- V20260905090000: Xử lý lô có kết quả kiểm nghiệm không đạt (QTN-30)
--
-- Thêm các cột xử lý loại bỏ (disposal) vào production_lot:
--   disposal_reason : lý do loại bỏ lô (bắt buộc khi dispose)
--   handling_measure: biện pháp xử lý lô (bắt buộc - TC-03)
--   disposal_note   : diễn giải chi tiết thêm (tùy chọn)
--   disposed_by     : người thực hiện loại bỏ lô (FK users)
--   disposed_at     : thời điểm loại bỏ lô
--
-- Tất cả các cột đều nullable để tương thích ngược với dữ liệu cũ
-- (lô DRAFT / PENDING chưa dispose). Không sửa/xóa dữ liệu
-- inspection history.
-- ============================================================

ALTER TABLE production_lot
    ADD COLUMN disposal_reason VARCHAR(100) NULL,
    ADD COLUMN handling_measure VARCHAR(1000) NULL,
    ADD COLUMN disposal_note VARCHAR(1000) NULL,
    ADD COLUMN disposed_by CHAR(36) NULL,
    ADD COLUMN disposed_at DATETIME NULL;

ALTER TABLE production_lot
    ADD CONSTRAINT fk_production_lot_disposed_by
    FOREIGN KEY (disposed_by) REFERENCES users(user_id);

CREATE INDEX idx_production_lot_disposed_by ON production_lot(disposed_by);
