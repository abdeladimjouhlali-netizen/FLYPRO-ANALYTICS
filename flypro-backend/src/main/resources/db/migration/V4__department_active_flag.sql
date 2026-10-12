-- =============================================================================
-- V4__department_active_flag.sql
-- Soft-delete support for departments (active flag).
-- =============================================================================

ALTER TABLE departments ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_departments_active ON departments (active);
CREATE INDEX idx_departments_name ON departments (name);
CREATE INDEX idx_departments_created_at ON departments (created_at);
