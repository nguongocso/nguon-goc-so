-- ============================================================
-- V33: Lịch sử xuất tem QR cho lô hàng (NCL-04-CN-005)
-- ============================================================

CREATE TABLE label_export_history (
    id CHAR(36) NOT NULL,
    shipment_id CHAR(36) NOT NULL,
    exported_by CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    exported_at DATETIME NOT NULL,
    start_index INT NOT NULL,
    end_index INT NOT NULL,
    quantity INT NOT NULL,
    label_size VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_label_export_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    CONSTRAINT fk_label_export_user FOREIGN KEY (exported_by) REFERENCES users(user_id),
    CONSTRAINT fk_label_export_org FOREIGN KEY (organization_id) REFERENCES organizations(organization_id)
) ENGINE=InnoDB;

CREATE INDEX idx_label_export_shipment ON label_export_history(shipment_id);
CREATE INDEX idx_label_export_org ON label_export_history(organization_id);
