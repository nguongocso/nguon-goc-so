-- ============================================================
-- V4: Production lot (depends on organizations, farm_areas, product_categories, users)
-- ============================================================

CREATE TABLE production_lot (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    farm_area_id CHAR(36) NULL,
    product_category_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    expected_quantity DOUBLE NOT NULL,
    expected_quantity_unit VARCHAR(20),
    actual_quantity DOUBLE NULL,
    planting_date DATE NULL,
    harvest_date DATE NULL,
    status VARCHAR(255) NOT NULL,
    approval_notes VARCHAR(255) NULL,
    created_by CHAR(36) NULL,
    approved_by CHAR(36) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT pk_production_lot PRIMARY KEY (id),
    CONSTRAINT FK_PRODUCTION_LOT_ON_ORGANIZATION FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT FK_PRODUCTION_LOT_ON_FARM_AREA FOREIGN KEY (farm_area_id) REFERENCES farm_areas (id),
    CONSTRAINT FK_PRODUCTION_LOT_ON_PRODUCT_CATEGORY FOREIGN KEY (product_category_id) REFERENCES product_categories (id),
    CONSTRAINT FK_PRODUCTION_LOT_ON_CREATED_BY FOREIGN KEY (created_by) REFERENCES users (user_id),
    CONSTRAINT FK_PRODUCTION_LOT_ON_APPROVED_BY FOREIGN KEY (approved_by) REFERENCES users (user_id)
) ENGINE=InnoDB;