-- ============================================================
-- V33: Inspection criterion catalog, category-criteria mapping,
-- requires_inspection flag, criterion_id reference
-- Story: NCL-09-CN-009
-- ============================================================

-- 1. Master inspection criterion catalog
CREATE TABLE inspection_criterion_catalog (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    max_threshold DECIMAL(12,4) NOT NULL,
    reference_standard VARCHAR(150),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_criterion_name_standard UNIQUE (name, reference_standard),
    CONSTRAINT chk_criterion_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_max_threshold_positive CHECK (max_threshold > 0)
) ENGINE=InnoDB;

-- Index for keyword search by name
CREATE INDEX idx_criterion_catalog_name ON inspection_criterion_catalog(name);

-- Index for filtering by status
CREATE INDEX idx_criterion_catalog_status ON inspection_criterion_catalog(status);

-- 2. Category-criteria mapping (N-N)
CREATE TABLE category_criteria (
    id CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,
    criterion_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_category_criteria UNIQUE (category_id, criterion_id),
    CONSTRAINT fk_category_criteria_category
        FOREIGN KEY (category_id)
        REFERENCES product_categories(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_category_criteria_criterion
        FOREIGN KEY (criterion_id)
        REFERENCES inspection_criterion_catalog(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. Add requires_inspection flag to product_categories
ALTER TABLE product_categories
    ADD COLUMN requires_inspection BOOLEAN NOT NULL DEFAULT FALSE;

-- 4. Add criterion_id to inspection_criteria for referencing the master catalog
--    (nullable for backward compatibility with existing free-text criteria)
ALTER TABLE inspection_criteria
    ADD COLUMN criterion_id BIGINT NULL,
    ADD CONSTRAINT fk_inspection_criteria_catalog
        FOREIGN KEY (criterion_id)
        REFERENCES inspection_criterion_catalog(id)
        ON DELETE SET NULL;

-- Index for referenced check queries
CREATE INDEX idx_inspection_criteria_criterion_id
    ON inspection_criteria(criterion_id);
