-- Schema creation for Input Materials (US NCL-09-CN-010)

CREATE TABLE IF NOT EXISTS input_materials (
    id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    material_group VARCHAR(50) NOT NULL,
    active_ingredient VARCHAR(255),
    unit VARCHAR(50) NOT NULL,
    quarantine_days INT NOT NULL DEFAULT 0,
    apply_to_all_crops BOOLEAN NOT NULL DEFAULT TRUE,
    reference_source TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by CHAR(36),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_input_materials PRIMARY KEY (id),
    CONSTRAINT uk_material_name_ingredient UNIQUE (name, active_ingredient)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_input_materials_group ON input_materials(material_group);
CREATE INDEX idx_input_materials_active ON input_materials(is_active);

CREATE TABLE IF NOT EXISTS input_material_crop_types (
    material_id CHAR(36) NOT NULL,
    crop_category_id CHAR(36) NOT NULL,
    CONSTRAINT pk_input_material_crop_types PRIMARY KEY (material_id, crop_category_id),
    CONSTRAINT fk_imct_material FOREIGN KEY (material_id) REFERENCES input_materials(id) ON DELETE CASCADE,
    CONSTRAINT fk_imct_crop FOREIGN KEY (crop_category_id) REFERENCES product_categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
