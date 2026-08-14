-- ============================================================
-- V1: Core authentication & authorization tables
-- ============================================================

CREATE TABLE roles (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE permissions (
    permission_id INT AUTO_INCREMENT PRIMARY KEY,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    UNIQUE KEY uk_permission_resource_action (resource, action)
) ENGINE=InnoDB;

CREATE TABLE organizations (
    organization_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    address VARCHAR(255),
    phone VARCHAR(30),
    email VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_organizations PRIMARY KEY (organization_id)
) ENGINE=InnoDB;

CREATE TABLE users (
    user_id CHAR(36) NOT NULL,
    user_name VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (user_id)
) ENGINE=InnoDB;

CREATE TABLE organization_users (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    role_id INT NOT NULL,
    custom_permissions TEXT,
    joined_at DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT pk_organization_users PRIMARY KEY (id),
    CONSTRAINT fk_org_user_org FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_org_user_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_org_user_role FOREIGN KEY (role_id) REFERENCES roles (role_id),
    CONSTRAINT uk_org_user UNIQUE (organization_id, user_id)
) ENGINE=InnoDB;

CREATE TABLE invitations (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    expiry_date DATETIME NOT NULL,
    used_at DATETIME,
    created_by CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT pk_invitations PRIMARY KEY (id),
    CONSTRAINT fk_invitation_org FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_invitation_role FOREIGN KEY (role_id) REFERENCES roles (role_id),
    CONSTRAINT fk_invitation_user FOREIGN KEY (created_by) REFERENCES users (user_id)
) ENGINE=InnoDB;
