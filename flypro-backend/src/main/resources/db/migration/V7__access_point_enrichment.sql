-- Access Point: status, description, ip, location, unique name

ALTER TABLE access_points ADD COLUMN description VARCHAR(500) NULL;
ALTER TABLE access_points ADD COLUMN status VARCHAR(30) NULL;
ALTER TABLE access_points ADD COLUMN ip_address VARCHAR(45) NULL;
ALTER TABLE access_points ADD COLUMN location VARCHAR(255) NULL;

UPDATE access_points SET status = 'ACTIVE' WHERE status IS NULL;

ALTER TABLE access_points MODIFY COLUMN status VARCHAR(30) NOT NULL;

ALTER TABLE access_points ADD CONSTRAINT uk_access_points_name UNIQUE (name);

CREATE INDEX idx_access_points_name ON access_points (name);
CREATE INDEX idx_access_points_status ON access_points (status);
