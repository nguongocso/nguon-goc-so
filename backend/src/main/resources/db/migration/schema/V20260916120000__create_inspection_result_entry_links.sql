CREATE TABLE inspection_result_entry_links (
    id CHAR(36) NOT NULL,
    inspection_request_id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    testing_unit_id CHAR(36) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    token_prefix VARCHAR(16) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    used_ip VARCHAR(45) NULL,
    used_user_agent VARCHAR(500) NULL,
    created_by CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL,
    revoked_by CHAR(36) NULL,
    revoked_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_inspection_result_entry_link_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_inspection_result_entry_link_request
        FOREIGN KEY (inspection_request_id) REFERENCES inspection_requests(id),
    CONSTRAINT fk_inspection_result_entry_link_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    CONSTRAINT fk_inspection_result_entry_link_testing_unit
        FOREIGN KEY (testing_unit_id) REFERENCES testing_units(id),
    CONSTRAINT fk_inspection_result_entry_link_created_by
        FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_inspection_result_entry_link_revoked_by
        FOREIGN KEY (revoked_by) REFERENCES users(user_id),
    INDEX idx_result_entry_link_request_status (inspection_request_id, status),
    INDEX idx_result_entry_link_organization_created (organization_id, created_at),
    INDEX idx_result_entry_link_status_expiry (status, expires_at)
) ENGINE=InnoDB;

ALTER TABLE inspection_criterion_results
    ADD COLUMN entry_source VARCHAR(32) NOT NULL DEFAULT 'COOPERATIVE_MANUAL' AFTER file_path,
    ADD COLUMN portal_link_id CHAR(36) NULL AFTER entry_source,
    MODIFY COLUMN created_by CHAR(36) NULL;

ALTER TABLE inspection_criterion_results
    ADD CONSTRAINT fk_inspection_criterion_result_portal_link
        FOREIGN KEY (portal_link_id) REFERENCES inspection_result_entry_links(id);

CREATE INDEX idx_inspection_criterion_result_portal_link
    ON inspection_criterion_results(portal_link_id);
