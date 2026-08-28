-- ============================================================
-- V48: Lot assignments (phân công thành viên vào lô sản xuất)
--      (NCL-01-CN-009 / QTN-32 — data layer phục vụ kiểm soát
--      phân công lô trước khi vô hiệu hóa thành viên)
-- Depends on: production_lot, users, organizations
-- ============================================================

-- Gán thành viên phụ trách một lô sản xuất.
-- Mỗi lần chuyển giao tạo bản ghi mới; bản ghi cũ chỉ bị vô hiệu
-- (active = FALSE, released_at/released_by) để bảo toàn lịch sử
-- phân công — không DELETE (QTN-32 mục 5).
CREATE TABLE lot_assignments (
    id CHAR(36) NOT NULL,
    lot_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_by CHAR(36) NULL,
    assigned_at DATETIME(6) NOT NULL,
    released_at DATETIME(6) NULL,
    released_by CHAR(36) NULL,
    CONSTRAINT pk_lot_assignments PRIMARY KEY (id),
    CONSTRAINT fk_lot_assignment_lot FOREIGN KEY (lot_id) REFERENCES production_lot (id),
    CONSTRAINT fk_lot_assignment_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_lot_assignment_organization FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_lot_assignment_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (user_id),
    CONSTRAINT fk_lot_assignment_released_by FOREIGN KEY (released_by) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_lot_assignment_lot ON lot_assignments (lot_id);
CREATE INDEX idx_lot_assignment_user ON lot_assignments (user_id);
CREATE INDEX idx_lot_assignment_org_active ON lot_assignments (organization_id, active);
