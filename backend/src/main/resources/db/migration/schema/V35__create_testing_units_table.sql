CREATE TABLE testing_units (
                                 id CHAR(36) NOT NULL,
                                 name VARCHAR(255) NOT NULL,
                                 accreditation_code VARCHAR(100) NOT NULL,
                                 contact_info VARCHAR(500) NULL,
                                 accreditation_expiry_date DATE NULL,
                                 is_active BOOLEAN NOT NULL DEFAULT 1,
                                 created_at DATETIME NOT NULL,
                                 updated_at DATETIME NULL,

                                 PRIMARY KEY (id),

                                 CONSTRAINT uq_testing_units_name
                                     UNIQUE (name)
);

-- NCL-11-CN-006 Phase 1: liên kết yêu cầu kiểm nghiệm với đơn vị kiểm nghiệm trong danh mục.
-- Cột nullable để tương thích ngược với các yêu cầu cũ nhập tự do.
ALTER TABLE inspection_requests
    ADD COLUMN testing_unit_id CHAR(36) NULL;

ALTER TABLE inspection_requests
    ADD CONSTRAINT fk_inspection_request_testing_unit
        FOREIGN KEY (testing_unit_id)
            REFERENCES testing_units(id);