CREATE TABLE trace_codes (
    id CHAR(36) PRIMARY KEY,

    shipment_id CHAR(36) NOT NULL,

    code_value VARCHAR(100) NOT NULL UNIQUE,

    qr_image VARCHAR(500),

    status VARCHAR(20) NOT NULL,

    activated_at DATETIME,
    activated_by CHAR(36),
    created_at DATETIME NOT NULL,

    CONSTRAINT fk_trace_code_shipment
        FOREIGN KEY (shipment_id)
        REFERENCES shipments(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_trace_code_activated_by
        FOREIGN KEY (activated_by)
        REFERENCES users(user_id)
);