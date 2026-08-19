-- ============================================================
-- V9: Chain events, code ranges, product feedback
-- ============================================================

-- chain_events: matches ChainEvent entity
CREATE TABLE chain_events (
    id CHAR(36) NOT NULL,
    shipment_id CHAR(36),
    event_type VARCHAR(50) NOT NULL,
    event_data JSON,
    location GEOMETRY,
    recorded_at DATETIME NOT NULL,
    recorded_by CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL,
    parent_event_id CHAR(36),
    is_correction BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_chain_event_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    CONSTRAINT fk_chain_event_user FOREIGN KEY (recorded_by) REFERENCES users(user_id),
    CONSTRAINT fk_chain_event_parent FOREIGN KEY (parent_event_id) REFERENCES chain_events(id)
) ENGINE=InnoDB;

-- code_ranges: matches CodeRange entity
CREATE TABLE code_ranges (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    prefix VARCHAR(255) NOT NULL UNIQUE,
    from_number BIGINT,
    to_number BIGINT,
    total_limit BIGINT NOT NULL,
    used_count BIGINT NOT NULL DEFAULT 0,
    created_by CHAR(36),
    created_at DATETIME,
    updated_at DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_code_range_org FOREIGN KEY (organization_id) REFERENCES organizations(organization_id)
) ENGINE=InnoDB;

-- product_feedbacks: matches ProductFeedback entity
CREATE TABLE product_feedbacks (
    id CHAR(36) NOT NULL,
    production_lot_id CHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_feedback_production_lot FOREIGN KEY (production_lot_id) REFERENCES production_lot(id)
) ENGINE=InnoDB;