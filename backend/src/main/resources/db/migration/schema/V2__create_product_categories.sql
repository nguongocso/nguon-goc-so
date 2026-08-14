-- ============================================================
-- V2: Product categories (referenced by farm_areas, production_lot)
-- ============================================================

CREATE TABLE product_categories (
    id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category_group VARCHAR(100),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_product_categories PRIMARY KEY (id),
    CONSTRAINT uk_product_categories_name UNIQUE (name)
) ENGINE=InnoDB;