-- ============================================================
-- V20: Add hash fields to chain_events
-- for NCL-08-CN-006: Event chain integrity verification
-- ============================================================

ALTER TABLE chain_events
    ADD COLUMN hash VARCHAR(64) NULL,
    ADD COLUMN previous_hash VARCHAR(64) NULL;

-- Tạo index để truy vấn nhanh theo shipment + thời gian
CREATE INDEX idx_chain_events_shipment_id_recorded_at
    ON chain_events(shipment_id, recorded_at);