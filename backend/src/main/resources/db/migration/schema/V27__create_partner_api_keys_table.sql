-- ============================================================
-- V23: Partner API Keys (NCL-12-CN-001 - Issue and Revoke API Key for Third-Party)
-- ============================================================

CREATE TABLE partner_api_keys (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    partner_name VARCHAR(255) NOT NULL,
    key_prefix VARCHAR(32) NOT NULL,
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    rate_limit_per_hour INT NOT NULL,
    expires_at DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_calls BIGINT DEFAULT 0,
    failed_calls BIGINT DEFAULT 0,
    last_called_at DATETIME NULL,
    last_call_status INT NULL,
    last_call_ip VARCHAR(45) NULL,
    created_by CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL,
    revoked_by CHAR(36) NULL,
    revoked_at DATETIME NULL,
    CONSTRAINT pk_partner_api_keys PRIMARY KEY (id),
    CONSTRAINT fk_partner_api_keys_org FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_partner_api_keys_created_by FOREIGN KEY (created_by) REFERENCES users (user_id),
    CONSTRAINT fk_partner_api_keys_revoked_by FOREIGN KEY (revoked_by) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_partner_api_keys_org_status ON partner_api_keys (organization_id, status);
