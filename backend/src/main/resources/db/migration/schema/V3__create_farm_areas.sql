-- ============================================================
-- V3: Farm areas (depends on organizations + product_categories)
-- ============================================================

CREATE TABLE farm_areas (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    crop_type CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    area DECIMAL(10,2) NOT NULL,
    area_unit VARCHAR(20) NOT NULL,
    location POINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_farm_areas PRIMARY KEY (id),
    CONSTRAINT fk_farm_area_organization FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_farm_area_product_category FOREIGN KEY (crop_type) REFERENCES product_categories (id)
) ENGINE=InnoDB;

CREATE INDEX idx_farm_area_organization ON farm_areas (organization_id);
CREATE INDEX idx_farm_area_crop_type ON farm_areas (crop_type);
CREATE SPATIAL INDEX idx_farm_area_location ON farm_areas (location);