-- ============================================================
-- V5: Farm logs & attachments (depends on production_lot, users)
-- ============================================================

CREATE TABLE farm_logs (
    id CHAR(36) PRIMARY KEY,
    production_lot_id CHAR(36) NOT NULL,
    activity_type ENUM('PLANTING','WATERING','FERTILIZING','PESTICIDE','WEEDING','HARVESTING','OTHER') NOT NULL,
    material VARCHAR(255),
    quantity DOUBLE,
    unit VARCHAR(50),
    executed_date DATE NOT NULL,
    notes TEXT,
    created_by CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_farm_logs_production_lot FOREIGN KEY (production_lot_id) REFERENCES production_lot(id),
    CONSTRAINT fk_farm_logs_created_by FOREIGN KEY (created_by) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_farm_logs_production_lot ON farm_logs(production_lot_id);
CREATE INDEX idx_farm_logs_created_by ON farm_logs(created_by);
CREATE INDEX idx_farm_logs_executed_date ON farm_logs(executed_date);

CREATE TABLE farm_log_attachments (
    id CHAR(36) PRIMARY KEY,
    farm_log_id CHAR(36) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    description TEXT,
    uploaded_by CHAR(36) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (farm_log_id) REFERENCES farm_logs(id),
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
) ENGINE=InnoDB;