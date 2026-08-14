-- ============================================================
-- V6: Shipments (depends on production_lot, organizations, users)
-- ============================================================

CREATE TABLE shipments (
    id CHAR(36) PRIMARY KEY,
    production_lot_id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    total_quantity BIGINT NOT NULL,
    packaging_info VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    created_by CHAR(36),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_shipment_production_lot FOREIGN KEY (production_lot_id) REFERENCES production_lot(id),
    CONSTRAINT fk_shipment_organization FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    CONSTRAINT fk_shipment_created_by FOREIGN KEY (created_by) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE trace_codes (
    id CHAR(36) PRIMARY KEY,
    shipment_id CHAR(36) NOT NULL,
    code_value VARCHAR(100) NOT NULL UNIQUE,
    qr_image VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    activated_at DATETIME,
    activated_by CHAR(36),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_trace_code_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE,
    CONSTRAINT fk_trace_code_activated_by FOREIGN KEY (activated_by) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE recalls (
    id CHAR(36) NOT NULL,
    shipment_id CHAR(36) NOT NULL,
    reason TEXT NOT NULL,
    recalled_by CHAR(36) NOT NULL,
    recalled_at DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recall_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    CONSTRAINT fk_recall_user FOREIGN KEY (recalled_by) REFERENCES users(user_id)
) ENGINE=InnoDB;