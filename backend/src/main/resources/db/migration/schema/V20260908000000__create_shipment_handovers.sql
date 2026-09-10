-- Tạo bảng shipment_handovers để lưu thông tin phiếu bàn giao lô hàng
-- NCL-05-CN-008 + NCL-05-CN-009
--
-- LƯU Ý: KHÔNG khai báo COLLATE=utf8mb4_0900_ai_ci vì hai lí do:
--   1. Đây là collation chỉ tồn tại trên MySQL 8.0. Trên MariaDB câu lệnh
--      CREATE TABLE thất bại với lỗi "Unknown collation", Flyway ghi nhận
--      migration failed (success = 0) và chặn mọi lần khởi động về sau.
--   2. Các bảng cha (shipments, organizations, users) đang dùng
--      utf8mb4_general_ci; khai báo collation lệch sẽ gây lỗi errno 150
--      (foreign key mismatch) ngay cả trên MySQL 8.0.
-- Bảng sẽ kế thừa collation mặc định của database, đồng bộ với các bảng còn lại.
--
-- Dùng CREATE TABLE IF NOT EXISTS để migration có thể chạy lại an toàn sau một
-- lần thất bại giữa chừng (MariaDB/MySQL không hỗ trợ DDL transaction).

CREATE TABLE IF NOT EXISTS shipment_handovers (
    id CHAR(36) PRIMARY KEY,
    shipment_id CHAR(36) NOT NULL,
    from_organization_id CHAR(36) NOT NULL,
    to_organization_id CHAR(36) NOT NULL,
    quantity BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL COMMENT 'PENDING_CONFIRMATION, ACCEPTED, REJECTED, EXPIRED, CANCELLED',
    planned_at DATETIME NULL,
    vehicle_info VARCHAR(255) NULL,
    carrier_name VARCHAR(255) NULL,
    note TEXT NULL,
    expires_at DATETIME NOT NULL,
    attachment_path VARCHAR(512) NULL,
    created_by CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL,
    confirmed_by CHAR(36) NULL,
    confirmed_at DATETIME NULL,
    rejected_by CHAR(36) NULL,
    rejected_at DATETIME NULL,
    cancel_reason TEXT NULL,
    cancelled_by CHAR(36) NULL,
    cancelled_at DATETIME NULL,

    CONSTRAINT fk_handover_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    CONSTRAINT fk_handover_from_org FOREIGN KEY (from_organization_id) REFERENCES organizations(organization_id),
    CONSTRAINT fk_handover_to_org FOREIGN KEY (to_organization_id) REFERENCES organizations(organization_id),
    CONSTRAINT fk_handover_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_handover_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users(user_id),
    CONSTRAINT fk_handover_rejected_by FOREIGN KEY (rejected_by) REFERENCES users(user_id),
    CONSTRAINT fk_handover_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Indexes
CREATE INDEX idx_handover_shipment ON shipment_handovers(shipment_id);
CREATE INDEX idx_handover_to_org ON shipment_handovers(to_organization_id);
CREATE INDEX idx_handover_from_org ON shipment_handovers(from_organization_id);
CREATE INDEX idx_handover_status ON shipment_handovers(status);
CREATE INDEX idx_handover_expires_at ON shipment_handovers(expires_at);
CREATE INDEX idx_handover_status_expires ON shipment_handovers(status, expires_at);
