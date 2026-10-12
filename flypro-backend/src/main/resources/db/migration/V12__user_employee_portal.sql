-- Lien User ↔ Employee pour le portail employé
ALTER TABLE users
    ADD COLUMN employee_id BIGINT NULL AFTER role_id;

ALTER TABLE users
    ADD CONSTRAINT uk_users_employee_id UNIQUE (employee_id);

ALTER TABLE users
    ADD CONSTRAINT fk_users_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id)
        ON DELETE SET NULL;

CREATE INDEX idx_users_employee_id ON users (employee_id);
