-- ============================================================
-- V28: Suspicious cases table
-- ============================================================
-- Mỗi case đại diện cho một đợt nghi vấn tài khoản khi người dùng
-- có >= 5 login anomalies trong 24 giờ.
--
-- Các trường bắt buộc phải khớp với entity SuspiciousCase.
-- ============================================================

CREATE TABLE suspicious_cases (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    anomaly_count INT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    resolved_at TIMESTAMP(3) NULL,
    first_detected_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_detected_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT pk_suspicious_cases
        PRIMARY KEY (id),

    CONSTRAINT fk_suspicious_cases_user
        FOREIGN KEY (user_id)
            REFERENCES users (user_id),

    CONSTRAINT fk_suspicious_cases_organization
        FOREIGN KEY (organization_id)
            REFERENCES organizations (organization_id),

    INDEX idx_suspicious_cases_org_last_detected
        (organization_id, last_detected_at DESC),

    INDEX idx_suspicious_cases_user_last_detected
        (user_id, last_detected_at DESC),

    INDEX idx_suspicious_cases_status
        (status)
) ENGINE=InnoDB;
