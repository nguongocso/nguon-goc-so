-- ============================================================
-- V28: Help Content (NCL-01-CN-006 - In-App User Guidance)
-- ============================================================

CREATE TABLE help_content (
    id CHAR(36) NOT NULL,
    screen_key VARCHAR(100) NOT NULL,
    role_code VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    steps TEXT NOT NULL,
    example_data TEXT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_help_content PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_help_content_screen_role ON help_content (screen_key, role_code);