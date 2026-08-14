-- ============================================================
-- V11: Dossier export history, production lot import history, offline sync
-- ============================================================

-- dossier_export_history: matches DossierExportHistory entity
CREATE TABLE dossier_export_history (
    id CHAR(36) NOT NULL,
    shipment_id CHAR(36) NOT NULL,
    exporter_id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    exported_at DATETIME NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    status VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    PRIMARY KEY (id),
    CONSTRAINT fk_export_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    CONSTRAINT fk_export_exporter FOREIGN KEY (exporter_id) REFERENCES users(user_id),
    CONSTRAINT fk_export_org FOREIGN KEY (organization_id) REFERENCES organizations(organization_id)
) ENGINE=InnoDB;

-- production_lot_import_history: matches ProductionLotImportHistory entity
CREATE TABLE production_lot_import_history (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    imported_by CHAR(36) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    total_rows INT NOT NULL,
    success_count INT NOT NULL,
    failed_count INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    imported_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_import_org FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    CONSTRAINT fk_import_user FOREIGN KEY (imported_by) REFERENCES users(user_id)
) ENGINE=InnoDB;

-- offline_sync_logs: matches OfflineSyncLog entity
CREATE TABLE offline_sync_logs (
    id CHAR(36) NOT NULL,
    sync_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    offline_event_id CHAR(36) NOT NULL UNIQUE,
    production_lot_id CHAR(36),
    shipment_id CHAR(36),
    event_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason TEXT,
    synced_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sync_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_sync_status (status)
) ENGINE=InnoDB;