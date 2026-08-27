-- ============================================================
-- V41: Add label cancellation history and trace_codes cancellation fields
-- for US NCL-04-CN-006: Cancel damaged/printed-error labels
-- ============================================================

ALTER TABLE trace_codes
    ADD COLUMN cancelled_at TIMESTAMP NULL,
    ADD COLUMN cancelled_by CHAR(36) NULL,
    ADD COLUMN cancel_reason_type VARCHAR(50) NULL,
    ADD COLUMN cancel_reason TEXT NULL;

ALTER TABLE trace_codes
    ADD CONSTRAINT fk_trace_codes_cancelled_by
        FOREIGN KEY (cancelled_by) REFERENCES users(user_id);

CREATE TABLE IF NOT EXISTS label_cancellation_history (
    id CHAR(36) NOT NULL,
    shipment_id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    cancelled_by CHAR(36) NOT NULL,
    cancelled_at DATETIME NOT NULL,
    quantity INT NOT NULL,
    cancellation_type VARCHAR(20) NOT NULL,
    range_from_code VARCHAR(100) NULL,
    range_to_code VARCHAR(100) NULL,
    reason_type VARCHAR(50) NOT NULL,
    reason_note TEXT NULL,
    CONSTRAINT pk_label_cancellation_history PRIMARY KEY (id),
    CONSTRAINT fk_lch_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    CONSTRAINT fk_lch_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users(user_id),
    CONSTRAINT fk_lch_organization FOREIGN KEY (organization_id) REFERENCES organizations(organization_id)
);

CREATE INDEX idx_lch_shipment_id ON label_cancellation_history(shipment_id);
CREATE INDEX idx_lch_organization_id ON label_cancellation_history(organization_id);
