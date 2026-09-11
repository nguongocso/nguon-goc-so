-- ============================================================
-- V20260910000000: RecallCase + RecallLotResult (NCL-08-CN-012)
-- Depends on: production_lot, organizations, shipments, users
-- ============================================================

CREATE TABLE IF NOT EXISTS recall_cases (
    id CHAR(36) NOT NULL,
    case_code VARCHAR(40) NOT NULL,
    production_lot_id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    closed_by CHAR(36) NULL,
    closed_at DATETIME NULL,
    corrective_measures TEXT NULL,
    attachments TEXT NULL,
    CONSTRAINT pk_recall_cases PRIMARY KEY (id),
    CONSTRAINT uq_recall_cases_case_code UNIQUE (case_code),
    CONSTRAINT fk_recall_case_on_lot FOREIGN KEY (production_lot_id) REFERENCES production_lot (id),
    CONSTRAINT fk_recall_case_on_org FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_recall_case_on_closed_by FOREIGN KEY (closed_by) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_recall_cases_status ON recall_cases (status);
CREATE INDEX idx_recall_cases_lot ON recall_cases (production_lot_id);
CREATE INDEX idx_recall_cases_org ON recall_cases (organization_id);

CREATE TABLE IF NOT EXISTS recall_lot_results (
    id CHAR(36) NOT NULL,
    recall_case_id CHAR(36) NOT NULL,
    shipment_id CHAR(36) NOT NULL,
    resolution VARCHAR(30) NOT NULL,
    recovered_quantity DECIMAL(18,3) NULL,
    notes TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_recall_lot_results PRIMARY KEY (id),
    CONSTRAINT uq_recall_result_case_shipment UNIQUE (recall_case_id, shipment_id),
    CONSTRAINT fk_recall_result_on_case FOREIGN KEY (recall_case_id) REFERENCES recall_cases (id) ON DELETE CASCADE,
    CONSTRAINT fk_recall_result_on_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_recall_result_case ON recall_lot_results (recall_case_id);
CREATE INDEX idx_recall_result_shipment ON recall_lot_results (shipment_id);
