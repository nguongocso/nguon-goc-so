-- ============================================================
-- V63: Cultivation milestone catalog + product-category-milestone mapping
-- Story: NCL-09-CN-011
-- ============================================================

-- 1. Master cultivation milestone catalog
CREATE TABLE cultivation_milestone_catalog (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    activity_type VARCHAR(30) NOT NULL,
    expected_days_from_planting INT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_milestone_name_activity UNIQUE (name, activity_type),
    CONSTRAINT chk_milestone_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB;

CREATE INDEX idx_milestone_catalog_name ON cultivation_milestone_catalog(name);
CREATE INDEX idx_milestone_catalog_activity_type ON cultivation_milestone_catalog(activity_type);
CREATE INDEX idx_milestone_catalog_status ON cultivation_milestone_catalog(status);

-- 2. Product-category-milestone mapping (N-N with optional standard scope)
CREATE TABLE product_category_milestones (
    id CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,
    milestone_id BIGINT NOT NULL,
    standard_id CHAR(36),
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_category_milestone_standard UNIQUE (category_id, milestone_id, standard_id),
    CONSTRAINT fk_pcm_category
        FOREIGN KEY (category_id)
        REFERENCES product_categories(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_pcm_milestone
        FOREIGN KEY (milestone_id)
        REFERENCES cultivation_milestone_catalog(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_pcm_standard
        FOREIGN KEY (standard_id)
        REFERENCES standards(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_pcm_category ON product_category_milestones(category_id);
CREATE INDEX idx_pcm_milestone ON product_category_milestones(milestone_id);
CREATE INDEX idx_pcm_standard ON product_category_milestones(standard_id);
