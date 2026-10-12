-- Demandes de congé employé et impact sur le contrôle d'accès
CREATE TABLE leave_requests (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id     BIGINT NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    type            VARCHAR(30) NOT NULL,
    reason          VARCHAR(1000) NULL,
    status          VARCHAR(30) NOT NULL,
    review_comment  VARCHAR(1000) NULL,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    created_by      VARCHAR(100) NULL,
    updated_by      VARCHAR(100) NULL,
    CONSTRAINT fk_leave_requests_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_leave_requests_employee_id ON leave_requests (employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests (status);
CREATE INDEX idx_leave_requests_dates ON leave_requests (start_date, end_date);
