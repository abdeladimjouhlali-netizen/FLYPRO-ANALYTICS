package com.flypro.analytics.zone.repository;

import com.flypro.analytics.common.test.FlyproDataJpaTest;
import com.flypro.analytics.zone.dto.ZoneSearchCriteria;
import com.flypro.analytics.zone.entity.Zone;
import com.flypro.analytics.zone.specification.ZoneSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@FlyproDataJpaTest
class ZoneRepositoryTest {

    @Autowired
    private ZoneRepository zoneRepository;

    @Test
    @DisplayName("persists zone and uniqueness lookups")
    void saveAndExists() {
        Zone zone = newZone("Lobby", "Main", "0", false);
        Zone saved = zoneRepository.saveAndFlush(zone);

        assertThat(saved.getId()).isNotNull();
        assertThat(zoneRepository.existsByNameIgnoreCase("lobby")).isTrue();
        assertThat(zoneRepository.hasAccessPoints(saved.getId())).isFalse();
        assertThat(zoneRepository.isAssignedToAnyProfile(saved.getId())).isFalse();
    }

    @Test
    @DisplayName("specification filters by sensitive and building")
    void searchSpecification() {
        zoneRepository.save(newZone("Server Room", "North", "3", true));
        zoneRepository.save(newZone("Lobby", "South", "0", false));
        zoneRepository.flush();

        ZoneSearchCriteria criteria = new ZoneSearchCriteria(null, "North", null, true, null);
        Page<Zone> page = zoneRepository.findAll(
                ZoneSpecification.withCriteria(criteria),
                PageRequest.of(0, 10, Sort.by("name"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getName()).isEqualTo("Server Room");
    }

    private Zone newZone(String name, String building, String floor, boolean sensitive) {
        Instant now = Instant.now();
        Zone zone = Zone.builder()
                .name(name)
                .building(building)
                .floor(floor)
                .sensitive(sensitive)
                .description(name + " desc")
                .build();
        zone.setCreatedAt(now);
        zone.setUpdatedAt(now);
        return zone;
    }
}
