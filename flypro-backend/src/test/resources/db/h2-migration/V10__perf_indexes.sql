-- =============================================================================
-- V10 – Additive performance indexes (H2 test twin)
-- =============================================================================

CREATE INDEX idx_access_events_ts_result ON access_events (event_timestamp, result);
CREATE INDEX idx_access_events_employee_ts ON access_events (employee_id, event_timestamp);
CREATE INDEX idx_access_events_zone_ts ON access_events (zone_id, event_timestamp);
CREATE INDEX idx_alerts_status_severity ON alerts (status, severity);
CREATE INDEX idx_alerts_created_status ON alerts (created_date, status);
