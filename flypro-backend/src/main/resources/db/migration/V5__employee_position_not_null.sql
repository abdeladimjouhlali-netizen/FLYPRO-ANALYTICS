-- =============================================================================
-- V5__employee_position_not_null.sql
-- Position becomes mandatory for employees.
-- =============================================================================

UPDATE employees SET position = 'UNSPECIFIED' WHERE position IS NULL;

ALTER TABLE employees MODIFY COLUMN position VARCHAR(150) NOT NULL;
