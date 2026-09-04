-- ============================================================
-- V62: NCL-02-CN-006 - Hủy lô sản xuất và ghi lý do
-- Thêm các cột hủy lô vào production_lot:
--   cancellation_reason : lý do hủy (mất mùa do thời tiết, sâu bệnh, khai báo nhầm, lý do khác)
--   cancellation_note   : diễn giải chi tiết khi hủy (bắt buộc)
--   cancelled_by        : người thực hiện hủy lô (FK users)
--   cancelled_at        : thời điểm hủy lô
-- ============================================================

ALTER TABLE production_lot
    ADD COLUMN cancellation_reason VARCHAR(100) NULL,
    ADD COLUMN cancellation_note VARCHAR(1000) NULL,
    ADD COLUMN cancelled_by CHAR(36) NULL,
    ADD COLUMN cancelled_at DATETIME NULL;

ALTER TABLE production_lot
    ADD CONSTRAINT fk_production_lot_cancelled_by
    FOREIGN KEY (cancelled_by) REFERENCES users(user_id);

CREATE INDEX idx_production_lot_cancelled_by ON production_lot(cancelled_by);