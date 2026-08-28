-- ============================================================
-- V49: Accreditation scope of testing units (NCL-11-CN-006 Phase 2)
--
-- Mỗi đơn vị kiểm nghiệm có thể khai báo phạm vi công nhận:
-- tập chỉ tiêu kiểm nghiệm (danh mục dùng chung, NCL-09-CN-009)
-- mà đơn vị được công nhận thực hiện.
--
-- Khi tạo yêu cầu kiểm nghiệm chọn đơn vị từ danh mục, hệ thống
-- so sánh bộ chỉ tiêu đã chọn với phạm vi công nhận và lưu cảnh
-- báo (KHÔNG chặn tạo yêu cầu) để người kiểm định biết.
-- ============================================================

CREATE TABLE accreditation_scopes (
    id CHAR(36) NOT NULL,
    testing_unit_id CHAR(36) NOT NULL,
    criterion_id BIGINT NOT NULL,
    criterion_code VARCHAR(150) NOT NULL,
    criterion_name VARCHAR(150) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_accreditation_scope_unit_criterion
        UNIQUE (testing_unit_id, criterion_id),
    CONSTRAINT fk_accreditation_scope_unit
        FOREIGN KEY (testing_unit_id)
        REFERENCES testing_units(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_accreditation_scope_criterion
        FOREIGN KEY (criterion_id)
        REFERENCES inspection_criterion_catalog(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- Index tra cứu phạm vi công nhận theo đơn vị kiểm nghiệm
CREATE INDEX idx_accreditation_scope_unit
    ON accreditation_scopes(testing_unit_id);

-- Lưu cảnh báo phạm vi công nhận trên yêu cầu kiểm nghiệm
ALTER TABLE inspection_requests
    ADD COLUMN scope_warning BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN scope_warning_details VARCHAR(2000) NULL;
