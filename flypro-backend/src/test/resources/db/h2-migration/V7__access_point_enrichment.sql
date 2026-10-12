ALTER TABLE access_points ADD COLUMN description VARCHAR(500);
ALTER TABLE access_points ADD COLUMN status VARCHAR(30);
ALTER TABLE access_points ADD COLUMN ip_address VARCHAR(45);
ALTER TABLE access_points ADD COLUMN location VARCHAR(255);

UPDATE access_points SET status = 'ACTIVE' WHERE status IS NULL;

ALTER TABLE access_points ALTER COLUMN status SET NOT NULL;

ALTER TABLE access_points ADD CONSTRAINT uk_access_points_name UNIQUE (name);

CREATE INDEX idx_access_points_name ON access_points (name);
CREATE INDEX idx_access_points_status ON access_points (status);
