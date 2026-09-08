-- ============================================================
-- V20260908120000: Bulk recall requests (NCL-08-CN-011)
-- Thu hồi theo phạm vi ảnh hưởng của lô sản xuất
--
-- LƯU Ý: version V20260908000000 đã được dùng bởi migration
-- "rename recalling count to recalled count" nên phải dùng
-- version mới (20260908120000) để Flyway nhận diện được migration mới.
-- ============================================================

-- Bảng đề nghị thu hồi hàng loạt
CREATE TABLE IF NOT EXISTS bulk_recall_requests (
    id CHAR(36) NOT NULL PRIMARY KEY,
    production_lot_id CHAR(36) NOT NULL,
    reason TEXT NOT NULL,
    evidence TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_by CHAR(36) NOT NULL,
    requested_at DATETIME NOT NULL,
    approved_by CHAR(36),
    approved_at DATETIME,
    approval_remarks TEXT,
    rejected_by CHAR(36),
    rejected_at DATETIME,
    rejection_reason TEXT,
    -- Cột sinh chỉ có giá trị khi status = 'PENDING' (convention giống
    -- V20260907195000): mỗi lô sản xuất chỉ được có tối đa một đề nghị
    -- thu hồi đang chờ duyệt. MySQL cho phép nhiều giá trị NULL trong
    -- unique index nên vẫn giữ được toàn bộ lịch sử các đề nghị đã xử lý.
    pending_production_lot_id CHAR(36)
        GENERATED ALWAYS AS (
            CASE WHEN status = 'PENDING' THEN production_lot_id ELSE NULL END
        ) STORED,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_bulk_recall_production_lot
        FOREIGN KEY (production_lot_id) REFERENCES production_lot(id),
    CONSTRAINT fk_bulk_recall_requested_by
        FOREIGN KEY (requested_by) REFERENCES users(user_id),
    CONSTRAINT fk_bulk_recall_approved_by
        FOREIGN KEY (approved_by) REFERENCES users(user_id),
    CONSTRAINT fk_bulk_recall_rejected_by
        FOREIGN KEY (rejected_by) REFERENCES users(user_id),
    CONSTRAINT uk_bulk_recall_pending_lot UNIQUE (pending_production_lot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Index cho tìm kiếm theo production lot và status
CREATE INDEX idx_bulk_recall_production_lot_status
    ON bulk_recall_requests(production_lot_id, status);

-- Index cho tìm kiếm theo người tạo
CREATE INDEX idx_bulk_recall_requested_by
    ON bulk_recall_requests(requested_by);

-- Ràng buộc "một đề nghị PENDING cho mỗi lô sản xuất" đã được định nghĩa
-- bằng generated column uk_bulk_recall_pending_lot ngay trong CREATE TABLE
-- (MySQL không hỗ trợ partial index có mệnh đề WHERE).

-- Bảng chi tiết lô hàng trong đề nghị thu hồi
CREATE TABLE IF NOT EXISTS bulk_recall_shipments (
    id CHAR(36) NOT NULL PRIMARY KEY,
    bulk_recall_request_id CHAR(36) NOT NULL,
    shipment_id CHAR(36) NOT NULL,
    included TINYINT(1) NOT NULL DEFAULT 1,
    exclusion_reason TEXT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_bulk_recall_shipment_request
        FOREIGN KEY (bulk_recall_request_id) REFERENCES bulk_recall_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_bulk_recall_shipment_shipment
        FOREIGN KEY (shipment_id) REFERENCES shipments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Index cho tìm kiếm theo bulk recall request
CREATE INDEX idx_bulk_recall_shipment_request
    ON bulk_recall_shipments(bulk_recall_request_id);

-- Index cho tìm kiếm theo shipment
CREATE INDEX idx_bulk_recall_shipment_shipment
    ON bulk_recall_shipments(shipment_id);