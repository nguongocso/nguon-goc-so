CREATE TABLE inspection_criteria (
    id CHAR(36) NOT NULL,
    inspection_request_id CHAR(36) NOT NULL,
    standard_id CHAR(36),
    criterion_code VARCHAR(100) NOT NULL,
    criterion_name VARCHAR(255) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_inspection_criterion_request
        FOREIGN KEY (inspection_request_id)
        REFERENCES inspection_requests(id),

    CONSTRAINT fk_inspection_criterion_standard
        FOREIGN KEY (standard_id)
        REFERENCES standards(id),

    CONSTRAINT uk_inspection_request_criterion
        UNIQUE (inspection_request_id, criterion_code)
);