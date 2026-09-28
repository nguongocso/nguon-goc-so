-- ============================================================
-- V20260907140000: Add print fields to trace_codes
-- for US NCL-04-CN-008: View and trace individual code status within a shipment
-- ============================================================

ALTER TABLE trace_codes
    ADD COLUMN printed_at TIMESTAMP NULL,
    ADD COLUMN print_batch_id VARCHAR(100) NULL;

CREATE INDEX idx_trace_codes_printed_at ON trace_codes(printed_at);
