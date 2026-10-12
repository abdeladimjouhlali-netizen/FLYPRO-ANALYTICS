-- Alert enrichment: type + resolved_by (H2)

ALTER TABLE alerts ADD COLUMN IF NOT EXISTS type VARCHAR(50);
UPDATE alerts SET type = 'SYSTEM_WARNING' WHERE type IS NULL;
ALTER TABLE alerts ALTER COLUMN type SET NOT NULL;

ALTER TABLE alerts ADD COLUMN IF NOT EXISTS resolved_by VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_alerts_type ON alerts (type);
CREATE INDEX IF NOT EXISTS idx_alerts_created_date ON alerts (created_date);
