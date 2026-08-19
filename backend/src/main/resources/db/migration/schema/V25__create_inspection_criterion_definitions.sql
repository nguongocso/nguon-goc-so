CREATE TABLE inspection_criterion_definitions (
    id INT NOT NULL AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    note TEXT,
    standard_id CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uq_inspection_criterion_definition_standard_code
        UNIQUE (standard_id, code),

    CONSTRAINT fk_inspection_criterion_definition_standard
        FOREIGN KEY (standard_id)
        REFERENCES standards(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);