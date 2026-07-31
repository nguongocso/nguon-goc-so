CREATE TABLE trace_code_scan_logs (
    id CHAR(36) NOT NULL,
    trace_code_id CHAR(36) NOT NULL,
    scanned_at DATETIME NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,
    location VARCHAR(255) NULL,
    is_abnormal TINYINT(1) NOT NULL DEFAULT 0,
    abnormal_reason VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_scan_logs_trace_code FOREIGN KEY (trace_code_id) REFERENCES trace_codes(id)
);

CREATE INDEX idx_scan_logs_trace_code ON trace_code_scan_logs(trace_code_id);
CREATE INDEX idx_scan_logs_scanned_at ON trace_code_scan_logs(scanned_at);
