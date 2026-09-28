-- ============================================================
-- NCL-782: Bổ sung quan hệ lô cha - lô con cho nghiệp vụ tách lô
-- ============================================================

ALTER TABLE shipments
    ADD COLUMN parent_shipment_id CHAR(36) NULL AFTER code_range_id,
    ADD COLUMN recipient_organization_id CHAR(36) NULL AFTER parent_shipment_id,
    ADD COLUMN split_at DATETIME NULL AFTER recipient_organization_id,
    ADD COLUMN split_by CHAR(36) NULL AFTER split_at,
    ADD CONSTRAINT fk_shipment_parent
        FOREIGN KEY (parent_shipment_id) REFERENCES shipments(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_shipment_recipient_organization
        FOREIGN KEY (recipient_organization_id) REFERENCES organizations(organization_id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_shipment_split_by
        FOREIGN KEY (split_by) REFERENCES users(user_id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_shipment_not_own_parent
        CHECK (parent_shipment_id IS NULL OR parent_shipment_id <> id);

CREATE INDEX idx_shipments_parent_shipment_id
    ON shipments(parent_shipment_id);

CREATE INDEX idx_shipments_recipient_status
    ON shipments(recipient_organization_id, status);
