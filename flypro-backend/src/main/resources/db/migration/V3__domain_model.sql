-- =============================================================================
-- V3__domain_model.sql
-- Domain persistence model (MySQL)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Extend auth tables with full audit columns
-- ---------------------------------------------------------------------------
ALTER TABLE roles ADD COLUMN created_at DATETIME(6) NULL;
ALTER TABLE roles ADD COLUMN updated_at DATETIME(6) NULL;
ALTER TABLE roles ADD COLUMN created_by VARCHAR(100) NULL;
ALTER TABLE roles ADD COLUMN updated_by VARCHAR(100) NULL;

UPDATE roles
SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP(6));

ALTER TABLE roles
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

ALTER TABLE users ADD COLUMN created_by VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN updated_by VARCHAR(100) NULL;

CREATE INDEX idx_users_email ON users (email);

-- ---------------------------------------------------------------------------
-- Departments
-- ---------------------------------------------------------------------------
CREATE TABLE departments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    code            VARCHAR(50)  NOT NULL,
    description     VARCHAR(500),
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT uk_departments_code UNIQUE (code),
    CONSTRAINT uk_departments_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_departments_code ON departments (code);

-- ---------------------------------------------------------------------------
-- Access profiles
-- ---------------------------------------------------------------------------
CREATE TABLE access_profiles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    description     VARCHAR(500),
    security_level  VARCHAR(30)  NOT NULL,
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT uk_access_profiles_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- Employees
-- ---------------------------------------------------------------------------
CREATE TABLE employees (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_number    VARCHAR(50)  NOT NULL,
    first_name         VARCHAR(100) NOT NULL,
    last_name          VARCHAR(100) NOT NULL,
    email              VARCHAR(255) NOT NULL,
    phone              VARCHAR(30),
    position           VARCHAR(150),
    status             VARCHAR(30)  NOT NULL,
    department_id      BIGINT       NOT NULL,
    access_profile_id  BIGINT,
    created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    CONSTRAINT uk_employees_employee_number UNIQUE (employee_number),
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT fk_employees_department
        FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT fk_employees_access_profile
        FOREIGN KEY (access_profile_id) REFERENCES access_profiles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_employees_employee_number ON employees (employee_number);
CREATE INDEX idx_employees_email ON employees (email);
CREATE INDEX idx_employees_department_id ON employees (department_id);
CREATE INDEX idx_employees_access_profile_id ON employees (access_profile_id);

-- ---------------------------------------------------------------------------
-- Badges
-- ---------------------------------------------------------------------------
CREATE TABLE badges (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    badge_number     VARCHAR(100) NOT NULL,
    status           VARCHAR(30)  NOT NULL,
    issue_date       DATE         NOT NULL,
    expiration_date  DATE,
    type             VARCHAR(30)  NOT NULL,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    CONSTRAINT uk_badges_badge_number UNIQUE (badge_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_badges_badge_number ON badges (badge_number);
CREATE INDEX idx_badges_status ON badges (status);

-- ---------------------------------------------------------------------------
-- Badge assignments
-- ---------------------------------------------------------------------------
CREATE TABLE badge_assignments (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    badge_id      BIGINT       NOT NULL,
    employee_id   BIGINT       NOT NULL,
    assigned_at   DATETIME(6)  NOT NULL,
    returned_at   DATETIME(6),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    CONSTRAINT fk_badge_assignments_badge
        FOREIGN KEY (badge_id) REFERENCES badges (id),
    CONSTRAINT fk_badge_assignments_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_badge_assignments_badge_id ON badge_assignments (badge_id);
CREATE INDEX idx_badge_assignments_employee_id ON badge_assignments (employee_id);
CREATE INDEX idx_badge_assignments_active ON badge_assignments (active);

-- ---------------------------------------------------------------------------
-- Zones
-- ---------------------------------------------------------------------------
CREATE TABLE zones (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    building      VARCHAR(150),
    floor         VARCHAR(50),
    is_sensitive  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    CONSTRAINT uk_zones_name_building_floor UNIQUE (name, building, floor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_zones_name ON zones (name);
CREATE INDEX idx_zones_building ON zones (building);

-- ---------------------------------------------------------------------------
-- Access points
-- ---------------------------------------------------------------------------
CREATE TABLE access_points (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    code          VARCHAR(50)  NOT NULL,
    type          VARCHAR(30)  NOT NULL,
    zone_id       BIGINT       NOT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    CONSTRAINT uk_access_points_code UNIQUE (code),
    CONSTRAINT fk_access_points_zone
        FOREIGN KEY (zone_id) REFERENCES zones (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_access_points_code ON access_points (code);
CREATE INDEX idx_access_points_zone_id ON access_points (zone_id);

-- ---------------------------------------------------------------------------
-- Access events
-- ---------------------------------------------------------------------------
CREATE TABLE access_events (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_timestamp   DATETIME(6)  NOT NULL,
    direction         VARCHAR(20)  NOT NULL,
    result            VARCHAR(20)  NOT NULL,
    refusal_reason    VARCHAR(500),
    source            VARCHAR(100),
    raw_payload       TEXT,
    badge_id          BIGINT,
    employee_id       BIGINT,
    access_point_id   BIGINT,
    zone_id           BIGINT,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    CONSTRAINT fk_access_events_badge
        FOREIGN KEY (badge_id) REFERENCES badges (id),
    CONSTRAINT fk_access_events_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_access_events_access_point
        FOREIGN KEY (access_point_id) REFERENCES access_points (id),
    CONSTRAINT fk_access_events_zone
        FOREIGN KEY (zone_id) REFERENCES zones (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_access_events_event_timestamp ON access_events (event_timestamp);
CREATE INDEX idx_access_events_badge_id ON access_events (badge_id);
CREATE INDEX idx_access_events_employee_id ON access_events (employee_id);
CREATE INDEX idx_access_events_access_point_id ON access_events (access_point_id);
CREATE INDEX idx_access_events_zone_id ON access_events (zone_id);
CREATE INDEX idx_access_events_result ON access_events (result);

-- ---------------------------------------------------------------------------
-- Import batches
-- ---------------------------------------------------------------------------
CREATE TABLE import_batches (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name           VARCHAR(255) NOT NULL,
    source              VARCHAR(100),
    status              VARCHAR(30)  NOT NULL,
    total_records       INTEGER      NOT NULL DEFAULT 0,
    valid_records       INTEGER      NOT NULL DEFAULT 0,
    invalid_records     INTEGER      NOT NULL DEFAULT 0,
    duplicate_records   INTEGER      NOT NULL DEFAULT 0,
    started_at          DATETIME(6),
    finished_at         DATETIME(6),
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- Data quality issues
-- ---------------------------------------------------------------------------
CREATE TABLE data_quality_issues (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    line_number      INTEGER,
    field_name       VARCHAR(100),
    message          VARCHAR(1000) NOT NULL,
    severity         VARCHAR(30)   NOT NULL,
    import_batch_id  BIGINT        NOT NULL,
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    CONSTRAINT fk_data_quality_issues_import_batch
        FOREIGN KEY (import_batch_id) REFERENCES import_batches (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_data_quality_issues_import_batch_id ON data_quality_issues (import_batch_id);
CREATE INDEX idx_data_quality_issues_severity ON data_quality_issues (severity);

-- ---------------------------------------------------------------------------
-- Alerts
-- ---------------------------------------------------------------------------
CREATE TABLE alerts (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    description      VARCHAR(2000),
    severity         VARCHAR(30)  NOT NULL,
    status           VARCHAR(30)  NOT NULL,
    created_date     DATETIME(6)  NOT NULL,
    resolved_date    DATETIME(6),
    employee_id      BIGINT,
    badge_id         BIGINT,
    access_event_id  BIGINT,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    CONSTRAINT fk_alerts_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_alerts_badge
        FOREIGN KEY (badge_id) REFERENCES badges (id),
    CONSTRAINT fk_alerts_access_event
        FOREIGN KEY (access_event_id) REFERENCES access_events (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_alerts_status ON alerts (status);
CREATE INDEX idx_alerts_severity ON alerts (severity);
CREATE INDEX idx_alerts_employee_id ON alerts (employee_id);
CREATE INDEX idx_alerts_badge_id ON alerts (badge_id);
CREATE INDEX idx_alerts_access_event_id ON alerts (access_event_id);

-- ---------------------------------------------------------------------------
-- Anomaly scores
-- ---------------------------------------------------------------------------
CREATE TABLE anomaly_scores (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    score            DECIMAL(10, 6) NOT NULL,
    algorithm        VARCHAR(100)   NOT NULL,
    prediction       VARCHAR(100),
    access_event_id  BIGINT         NOT NULL,
    created_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    CONSTRAINT fk_anomaly_scores_access_event
        FOREIGN KEY (access_event_id) REFERENCES access_events (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_anomaly_scores_access_event_id ON anomaly_scores (access_event_id);
CREATE INDEX idx_anomaly_scores_algorithm ON anomaly_scores (algorithm);

-- ---------------------------------------------------------------------------
-- Audit logs
-- ---------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    action        VARCHAR(100) NOT NULL,
    entity_name   VARCHAR(150) NOT NULL,
    entity_id     VARCHAR(100),
    username      VARCHAR(100),
    ip_address    VARCHAR(45),
    details       TEXT,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_audit_logs_username ON audit_logs (username);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_name, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
