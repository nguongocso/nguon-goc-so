-- ============================================================
-- V20260914110000: Tạo bảng mẫu hồ sơ truy xuất (profile_templates)
-- User Story: NCL-07-CN-007
-- ============================================================

CREATE TABLE IF NOT EXISTS profile_templates (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    partner_name VARCHAR(255) NULL,
    description VARCHAR(500) NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by CHAR(36) NULL,
    CONSTRAINT pk_profile_templates PRIMARY KEY (id),
    CONSTRAINT fk_profile_template_org FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_profile_template_user FOREIGN KEY (created_by) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_profile_templates_org ON profile_templates (organization_id);
