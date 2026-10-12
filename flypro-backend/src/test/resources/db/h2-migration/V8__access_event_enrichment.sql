UPDATE access_events SET source = 'API' WHERE source IS NULL OR TRIM(source) = '';

ALTER TABLE access_events ALTER COLUMN source SET NOT NULL;

CREATE INDEX idx_access_events_source ON access_events (source);
