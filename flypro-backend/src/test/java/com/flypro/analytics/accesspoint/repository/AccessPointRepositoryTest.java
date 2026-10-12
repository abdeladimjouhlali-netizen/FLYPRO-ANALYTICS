package com.flypro.analytics.accesspoint.repository;

import com.flypro.analytics.accesspoint.dto.AccessPointSearchCriteria;
import com.flypro.analytics.accesspoint.entity.AccessPoint;
import com.flypro.analytics.accesspoint.specification.AccessPointSpecification;
import com.flypro.analytics.common.enums.AccessPointStatus;
import com.flypro.analytics.common.enums.AccessPointType;
import com.flypro.analytics.common.test.FlyproDataJpaTest;
import com.flypro.analytics.zone.entity.Zone;
import com.flypro.analytics.zone.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@FlyproDataJpaTest
class AccessPointRepositoryTest {

    @Autowired
    private AccessPointRepository accessPointRepository;
    @Autowired
    private ZoneRepository zoneRepository;

    private Zone zone;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        zone = Zone.builder()
                .name("Lobby")
                .building("North")
                .floor("0")
                .sensitive(false)
                .build();
        zone.setCreatedAt(now);
        zone.setUpdatedAt(now);
        zone = zoneRepository.saveAndFlush(zone);
    }

    @Test
    @DisplayName("persists access point and uniqueness lookups")
    void saveAndExists() {
        AccessPoint ap = newAccessPoint("Main Door", "AP-001", AccessPointType.DOOR, AccessPointStatus.ACTIVE);
        AccessPoint saved = accessPointRepository.saveAndFlush(ap);

        assertThat(saved.getId()).isNotNull();
        assertThat(accessPointRepository.existsByCodeIgnoreCase("ap-001")).isTrue();
        assertThat(accessPointRepository.existsByNameIgnoreCase("main door")).isTrue();
        assertThat(accessPointRepository.hasAccessEvents(saved.getId())).isFalse();
        assertThat(accessPointRepository.findByZoneIdOrderByNameAsc(zone.getId())).hasSize(1);
    }

    @Test
    @DisplayName("specification filters by type, status, building and keyword")
    void searchSpecification() {
        accessPointRepository.save(newAccessPoint("Door A", "AP-A", AccessPointType.DOOR, AccessPointStatus.ACTIVE));
        accessPointRepository.save(newAccessPoint("Gate B", "AP-B", AccessPointType.GATE, AccessPointStatus.INACTIVE));
        accessPointRepository.flush();

        AccessPointSearchCriteria criteria = new AccessPointSearchCriteria(
                null, null, AccessPointType.DOOR, AccessPointStatus.ACTIVE,
                zone.getId(), "North", "0", "door"
        );

        Page<AccessPoint> page = accessPointRepository.findAll(
                AccessPointSpecification.withCriteria(criteria),
                PageRequest.of(0, 10, Sort.by("name"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getCode()).isEqualTo("AP-A");
    }

    private AccessPoint newAccessPoint(String name, String code, AccessPointType type, AccessPointStatus status) {
        Instant now = Instant.now();
        AccessPoint ap = AccessPoint.builder()
                .name(name)
                .code(code)
                .type(type)
                .description(name)
                .status(status)
                .ipAddress("10.0.0.1")
                .location("Lobby")
                .zone(zone)
                .build();
        ap.setCreatedAt(now);
        ap.setUpdatedAt(now);
        return ap;
    }
}
