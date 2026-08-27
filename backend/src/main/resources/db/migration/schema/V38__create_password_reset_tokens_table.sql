-- ============================================================
-- V38: Create password_reset_tokens table for NCL-01-CN-008
-- ============================================================

CREATE TABLE password_reset_tokens (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    INDEX idx_prt_user_created (user_id, created_at),
    INDEX idx_prt_hash_used_expires (token_hash, is_used, expires_at)
) ENGINE=InnoDB;
