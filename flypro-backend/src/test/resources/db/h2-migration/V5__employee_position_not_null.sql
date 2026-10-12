UPDATE employees SET position = 'UNSPECIFIED' WHERE position IS NULL;

ALTER TABLE employees ALTER COLUMN position SET NOT NULL;
