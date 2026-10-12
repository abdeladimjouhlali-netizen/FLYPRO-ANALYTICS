ALTER TABLE zones ADD COLUMN description VARCHAR(500);

UPDATE zones SET building = 'UNKNOWN' WHERE building IS NULL OR TRIM(building) = '';
UPDATE zones SET floor = '0' WHERE floor IS NULL OR TRIM(floor) = '';

ALTER TABLE zones ALTER COLUMN building SET NOT NULL;
ALTER TABLE zones ALTER COLUMN floor SET NOT NULL;

ALTER TABLE zones DROP CONSTRAINT IF EXISTS uk_zones_name_building_floor;

ALTER TABLE zones ADD CONSTRAINT uk_zones_name UNIQUE (name);

CREATE TABLE access_profile_zones (
    access_profile_id BIGINT NOT NULL,
    zone_id           BIGINT NOT NULL,
    PRIMARY KEY (access_profile_id, zone_id),
    CONSTRAINT fk_access_profile_zones_profile
        FOREIGN KEY (access_profile_id) REFERENCES access_profiles (id),
    CONSTRAINT fk_access_profile_zones_zone
        FOREIGN KEY (zone_id) REFERENCES zones (id)
);

CREATE INDEX idx_access_profile_zones_profile_id ON access_profile_zones (access_profile_id);
CREATE INDEX idx_access_profile_zones_zone_id ON access_profile_zones (zone_id);
