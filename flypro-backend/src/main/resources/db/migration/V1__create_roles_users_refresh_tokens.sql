-- =============================================================================
-- V1__create_roles_users_refresh_tokens.sql
-- Schéma d'authentification : roles, users, refresh_tokens (MySQL)
-- =============================================================================

CREATE TABLE roles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(50)  NOT NULL,
    description     VARCHAR(255),
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    account_locked  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    role_id         BIGINT       NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_users_role_id ON users (role_id);

CREATE TABLE refresh_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    token           VARCHAR(768) NOT NULL,
    user_id         BIGINT       NOT NULL,
    expires_at      DATETIME(6)  NOT NULL,
    revoked         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
