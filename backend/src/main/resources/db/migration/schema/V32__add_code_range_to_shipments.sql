-- ============================================================
-- V32: Add code_range_id to shipments for code-range accounting
-- ============================================================

ALTER TABLE shipments
    ADD COLUMN code_range_id CHAR(36) NULL,
    ADD CONSTRAINT fk_shipment_code_range FOREIGN KEY (code_range_id) REFERENCES code_ranges(id);
