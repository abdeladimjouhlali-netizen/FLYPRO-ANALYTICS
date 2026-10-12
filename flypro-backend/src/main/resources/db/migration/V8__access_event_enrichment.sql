-- Access events: source required + index for filtering

UPDATE access_events SET source = 'API' WHERE source IS NULL OR TRIM(source) = '';

ALTER TABLE access_events MODIFY COLUMN source VARCHAR(100) NOT NULL;

CREATE INDEX idx_access_events_source ON access_events (source);
