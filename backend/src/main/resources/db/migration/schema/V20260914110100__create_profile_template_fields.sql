-- ============================================================
-- V20260914110100: Tạo bảng các trường trong mẫu hồ sơ (profile_template_fields)
-- User Story: NCL-07-CN-007
-- ============================================================

CREATE TABLE IF NOT EXISTS profile_template_fields (
    id CHAR(36) NOT NULL,
    template_id CHAR(36) NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    field_group VARCHAR(100) NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_profile_template_fields PRIMARY KEY (id),
    CONSTRAINT fk_ptf_template FOREIGN KEY (template_id) REFERENCES profile_templates (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_ptf_template ON profile_template_fields (template_id);
CREATE UNIQUE INDEX uq_ptf_template_field ON profile_template_fields (template_id, field_key);
