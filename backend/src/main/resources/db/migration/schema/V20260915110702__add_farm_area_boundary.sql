ALTER TABLE farm_areas
    ADD COLUMN boundary POLYGON AFTER location,
    ADD COLUMN calculated_area DECIMAL(10, 4) NULL AFTER area,
    ADD COLUMN boundary_updated_at DATETIME NULL AFTER calculated_area;
