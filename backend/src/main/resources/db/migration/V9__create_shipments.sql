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

    CONSTRAINT fk_shipment_production_lot
        FOREIGN KEY (production_lot_id)
        REFERENCES production_lot(id),

    CONSTRAINT fk_shipment_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(organization_id),

    CONSTRAINT fk_shipment_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(user_id)
);