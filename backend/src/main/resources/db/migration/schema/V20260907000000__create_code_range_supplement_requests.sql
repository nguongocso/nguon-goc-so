-- ============================================================
-- V20260907000000: Code range supplement requests (NCL-04-CN-007)
-- Yeu cau cap bo sung dai ma truy xuat: VT-02 tao, VT-01 duyet.
-- Khi duyet: tang total_limit cua CodeRange hien co (khong tao dai moi
-- vi prefix UNIQUE toan he thong).
-- Depends on: organizations, users
-- ============================================================

CREATE TABLE code_range_supplement_requests (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    requested_by CHAR(36) NOT NULL,
    requested_at DATETIME NOT NULL,
    requested_quantity BIGINT NOT NULL,
    approved_quantity BIGINT NULL,
    reason TEXT NOT NULL,
    evidence_event_ids TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    approved_by CHAR(36) NULL,
    approved_at DATETIME NULL,
    approval_remarks TEXT NULL,
    rejected_by CHAR(36) NULL,
    rejected_at DATETIME NULL,
    rejection_reason TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_code_range_supplement_requests PRIMARY KEY (id),
    CONSTRAINT fk_supplement_req_on_organization FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_supplement_req_on_requested_by FOREIGN KEY (requested_by) REFERENCES users (user_id),
    CONSTRAINT fk_supplement_req_on_approved_by FOREIGN KEY (approved_by) REFERENCES users (user_id),
    CONSTRAINT fk_supplement_req_on_rejected_by FOREIGN KEY (rejected_by) REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Index cho danh sach loc theo trang thai va chong trung PENDING theo to chuc
CREATE INDEX idx_supplement_requests_status ON code_range_supplement_requests (status);
CREATE INDEX idx_supplement_requests_org_status ON code_range_supplement_requests (organization_id, status);
