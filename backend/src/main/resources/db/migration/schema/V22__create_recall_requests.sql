-- ============================================================
-- V22: Recall requests (NCL-08-CN-008 - two-step recall workflow)
-- Depends on: production_lot, users
-- ============================================================

CREATE TABLE recall_requests (
    id CHAR(36) NOT NULL,
    production_lot_id CHAR(36) NOT NULL,
    requested_by CHAR(36) NOT NULL,
    requested_at DATETIME NOT NULL,
    reason TEXT NOT NULL,
    evidence TEXT NULL,
    status VARCHAR(20) NOT NULL,
    approved_by CHAR(36) NULL,
    approved_at DATETIME NULL,
    approval_remarks TEXT NULL,
    rejected_by CHAR(36) NULL,
    rejected_at DATETIME NULL,
    rejection_reason TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_recall_requests PRIMARY KEY (id),
    CONSTRAINT FK_RECALL_REQUEST_ON_PRODUCTION_LOT FOREIGN KEY (production_lot_id) REFERENCES production_lot (id),
    CONSTRAINT FK_RECALL_REQUEST_ON_REQUESTED_BY FOREIGN KEY (requested_by) REFERENCES users (user_id),
    CONSTRAINT FK_RECALL_REQUEST_ON_APPROVED_BY FOREIGN KEY (approved_by) REFERENCES users (user_id),
    CONSTRAINT FK_RECALL_REQUEST_ON_REJECTED_BY FOREIGN KEY (rejected_by) REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Index để tăng tốc truy vấn danh sách theo trạng thái và tra cứu theo lô
CREATE INDEX idx_recall_requests_status ON recall_requests (status);
CREATE INDEX idx_recall_requests_lot_status ON recall_requests (production_lot_id, status);

-- Ghi chú: cột production_lot.status là VARCHAR(255) lưu tên enum (Hibernate
-- @Enumerated(EnumType.STRING)), không phải kiểu ENUM của MySQL nên không cần
-- ALTER khi thêm giá trị RECALLED vào Java enum ProductionLotStatus.