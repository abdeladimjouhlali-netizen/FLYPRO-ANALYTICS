-- =============================================================================
-- V2__seed_roles_and_super_admin.sql
-- Rôles par défaut + compte SUPER_ADMIN (admin / Admin@123 — BCrypt)
-- =============================================================================

INSERT INTO roles (name, description) VALUES
    ('ROLE_SUPER_ADMIN', 'Full platform administration privileges'),
    ('ROLE_ADMIN', 'Administrative access to operational modules'),
    ('ROLE_ANALYST', 'Read and analytics access'),
    ('ROLE_EMPLOYEE', 'Standard employee access');

-- Password: Admin@123 (BCrypt strength 10)
INSERT INTO users (
    username,
    email,
    password,
    enabled,
    account_locked,
    created_at,
    updated_at,
    role_id
) VALUES (
    'admin',
    'admin@flypro.local',
    '$2a$10$69ibjKTxtOaIAM5gCV1rEOEb3I7Z/Y5e/5k64pgDKkVeaJuwL7cUi',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    (SELECT id FROM (SELECT id FROM roles WHERE name = 'ROLE_SUPER_ADMIN') AS role_super_admin)
);
