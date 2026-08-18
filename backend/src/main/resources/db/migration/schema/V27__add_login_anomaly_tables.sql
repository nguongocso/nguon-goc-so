-- ============================================================
-- V27: Login anomaly detection tables
-- ============================================================
-- NCL-01-CN-005
--
-- Chức năng:
-- 1. Ghi nhận mỗi lần đăng nhập thành công/thất bại
-- 2. Ghi nhận các bất thường khi đăng nhập
-- 3. Quản lý vòng đời khóa/mở khóa tài khoản
--
-- Lưu ý:
-- - users.user_id là CHAR(36)
-- - organizations.organization_id phải tương thích với CHAR(36)
-- - users.status đang là VARCHAR(50), KHÔNG chuyển sang ENUM
-- ============================================================


-- ============================================================
-- 1. LOGIN ATTEMPTS
-- ============================================================
-- Ghi nhận mỗi lần đăng nhập:
-- SUCCESS / FAILED
--
-- user_id có thể NULL vì username đăng nhập có thể không tồn tại.
-- ============================================================

CREATE TABLE login_attempts (
                                id CHAR(36) NOT NULL,
                                user_id CHAR(36) NULL,
                                username_input VARCHAR(255) NOT NULL,
                                result VARCHAR(50) NOT NULL,
                                ip_address VARCHAR(45) NOT NULL,
                                country_code VARCHAR(2) NULL,
                                is_new_country BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

                                CONSTRAINT pk_login_attempts
                                    PRIMARY KEY (id),

                                CONSTRAINT fk_login_attempts_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users (user_id),

                                INDEX idx_login_attempts_user_created
                                    (user_id, created_at DESC),

                                INDEX idx_login_attempts_user_result
                                    (user_id, result),

                                INDEX idx_login_attempts_user_country
                                    (user_id, country_code)

) ENGINE=InnoDB;


-- ============================================================
-- 2. LOGIN ANOMALIES
-- ============================================================
-- Ghi nhận các bất thường được phát hiện.
--
-- reason_code:
--   REPEATED_FAILED_LOGIN
--   UNUSUAL_COUNTRY
--
-- status:
--   OPEN
--   ACCOUNT_LOCKED
--   DISMISSED
-- ============================================================

CREATE TABLE login_anomalies (
                                 id CHAR(36) NOT NULL,
                                 user_id CHAR(36) NOT NULL,
                                 organization_id CHAR(36) NOT NULL,
                                 reason_code VARCHAR(50) NOT NULL,
                                 attempt_count INT NULL,
                                 ip_address VARCHAR(45) NOT NULL,
                                 country_code VARCHAR(2) NULL,
                                 detected_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                 status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
                                 notification_id CHAR(36) NULL,

                                 CONSTRAINT pk_login_anomalies
                                     PRIMARY KEY (id),

                                 CONSTRAINT fk_login_anomalies_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES users (user_id),

                                 CONSTRAINT fk_login_anomalies_org
                                     FOREIGN KEY (organization_id)
                                         REFERENCES organizations (organization_id),

                                 INDEX idx_login_anomalies_org_detected
                                     (organization_id, detected_at DESC),

                                 INDEX idx_login_anomalies_user_detected
                                     (user_id, detected_at DESC),

                                 INDEX idx_login_anomalies_status
                                     (status)

) ENGINE=InnoDB;


-- ============================================================
-- 3. ACCOUNT LOCKS
-- ============================================================
-- Ghi nhận vòng đời khóa/mở khóa tài khoản.
--
-- status:
--   LOCKED
--   UNLOCKED
--
-- locked_by:
--   User thực hiện khóa tài khoản.
--
-- unlocked_by:
--   User thực hiện mở khóa tài khoản.
--   NULL nếu tài khoản chưa được mở khóa.
-- ============================================================

CREATE TABLE account_locks (
                               id CHAR(36) NOT NULL,
                               user_id CHAR(36) NOT NULL,
                               anomaly_id CHAR(36) NULL,
                               locked_by CHAR(36) NOT NULL,
                               lock_reason VARCHAR(500) NULL,
                               locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                               lock_until TIMESTAMP(3) NULL,
                               permanent BOOLEAN NOT NULL DEFAULT FALSE,
                               unlocked_by CHAR(36) NULL,
                               unlocked_at TIMESTAMP(3) NULL,
                               status VARCHAR(50) NOT NULL DEFAULT 'LOCKED',

                               CONSTRAINT pk_account_locks
                                   PRIMARY KEY (id),

                               CONSTRAINT fk_account_locks_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users (user_id),

                               CONSTRAINT fk_account_locks_anomaly
                                   FOREIGN KEY (anomaly_id)
                                       REFERENCES login_anomalies (id),

                               CONSTRAINT fk_account_locks_locked_by
                                   FOREIGN KEY (locked_by)
                                       REFERENCES users (user_id),

                               CONSTRAINT fk_account_locks_unlocked_by
                                   FOREIGN KEY (unlocked_by)
                                       REFERENCES users (user_id),

                               INDEX idx_account_locks_user_status
                                   (user_id, status),

                               INDEX idx_account_locks_user_locked_at
                                   (user_id, locked_at DESC)

) ENGINE=InnoDB;


-- ============================================================
-- 4. USERS STATUS
-- ============================================================
-- users.status hiện tại là VARCHAR(50), vì vậy không cần
-- ALTER sang ENUM.
--
-- Application/Java enum có thể quản lý các giá trị:
--
--   ACTIVE
--   INACTIVE
--   LOCKED
--
-- Không thực hiện ALTER TABLE users tại migration này.
-- ============================================================