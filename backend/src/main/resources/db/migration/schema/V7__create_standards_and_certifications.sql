-- ============================================================
-- V7: Standards, certifications, production_lot_certifications
-- ============================================================

CREATE TABLE standards (
    id CHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    issuing_body VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL
) ENGINE=InnoDB;

-- certifications: matches Certification entity
CREATE TABLE certifications (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    standard_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    issuing_body VARCHAR(255),
    code VARCHAR(255) NOT NULL UNIQUE,
    issued_by VARCHAR(255),
    issue_date DATE,
    expiry_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cert_org FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    CONSTRAINT fk_cert_standard FOREIGN KEY (standard_id) REFERENCES standards(id)
) ENGINE=InnoDB;

CREATE TABLE production_lot_certifications (
    id CHAR(36) NOT NULL,
    production_lot_id CHAR(36) NOT NULL,
    certification_id CHAR(36) NOT NULL,
    attached_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attached_by CHAR(36),
    note TEXT,
    PRIMARY KEY (id),
    CONSTRAINT uk_lot_cert UNIQUE (production_lot_id, certification_id),
    CONSTRAINT fk_plc_lot FOREIGN KEY (production_lot_id) REFERENCES production_lot(id),
    CONSTRAINT fk_plc_cert FOREIGN KEY (certification_id) REFERENCES certifications(id),
    CONSTRAINT fk_plc_user FOREIGN KEY (attached_by) REFERENCES users(user_id)
) ENGINE=InnoDB;