CREATE TABLE IF NOT EXISTS label_export_history (
    id CHAR(36) NOT NULL,
    shipment_id CHAR(36) NOT NULL,
    exported_by CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    exported_at DATETIME NOT NULL,
    start_index INT NOT NULL,
    end_index INT NOT NULL,
    quantity INT NOT NULL,
    label_size VARCHAR(20) NOT NULL,
    CONSTRAINT pk_label_export_history PRIMARY KEY (id),
    CONSTRAINT fk_leh_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    CONSTRAINT fk_leh_exported_by FOREIGN KEY (exported_by) REFERENCES users(user_id),
    CONSTRAINT fk_leh_organization FOREIGN KEY (organization_id) REFERENCES organizations(organization_id)
);
