-- =============================================================================
-- V11__reports_notifications.sql
-- Reporting schedules + in-app notifications (Phase 5 – Step 19)
-- Does not alter existing domain tables.
-- =============================================================================

CREATE TABLE notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel         VARCHAR(30)  NOT NULL,
    type            VARCHAR(50)  NOT NULL,
    recipient       VARCHAR(255) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    body            TEXT         NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    severity        VARCHAR(30),
    related_entity  VARCHAR(100),
    related_id      VARCHAR(100),
    error_message   VARCHAR(1000),
    sent_at         DATETIME(6),
    read_at         DATETIME(6),
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    INDEX idx_notifications_status (status),
    INDEX idx_notifications_type (type),
    INDEX idx_notifications_recipient (recipient),
    INDEX idx_notifications_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE report_schedules (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(150) NOT NULL,
    report_type      VARCHAR(50)  NOT NULL,
    export_format    VARCHAR(20)  NOT NULL,
    report_kind      VARCHAR(30)  NOT NULL,
    frequency        VARCHAR(20)  NOT NULL,
    filters_json     TEXT,
    recipients       VARCHAR(1000) NOT NULL,
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    last_run_at      DATETIME(6),
    next_run_at      DATETIME(6),
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    INDEX idx_report_schedules_enabled (enabled),
    INDEX idx_report_schedules_next_run (next_run_at),
    INDEX idx_report_schedules_frequency (frequency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
