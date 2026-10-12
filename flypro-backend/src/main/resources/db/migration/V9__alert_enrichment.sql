-- Alert enrichment: type + resolved_by

ALTER TABLE alerts
    ADD COLUMN type VARCHAR(50) NULL AFTER status;

UPDATE alerts SET type = 'SYSTEM_WARNING' WHERE type IS NULL;

ALTER TABLE alerts
    MODIFY COLUMN type VARCHAR(50) NOT NULL;

ALTER TABLE alerts
    ADD COLUMN resolved_by VARCHAR(100) NULL AFTER resolved_date;

CREATE INDEX idx_alerts_type ON alerts (type);
CREATE INDEX idx_alerts_created_date ON alerts (created_date);
