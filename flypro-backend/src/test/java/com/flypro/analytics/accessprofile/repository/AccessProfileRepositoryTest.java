package com.flypro.analytics.accessprofile.repository;

import com.flypro.analytics.accessprofile.dto.AccessProfileSearchCriteria;
import com.flypro.analytics.accessprofile.entity.AccessProfile;
import com.flypro.analytics.accessprofile.specification.AccessProfileSpecification;
import com.flypro.analytics.common.enums.SecurityLevel;
import com.flypro.analytics.common.test.FlyproDataJpaTest;
import com.flypro.analytics.zone.entity.Zone;
import com.flypro.analytics.zone.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@FlyproDataJpaTest
class AccessProfileRepositoryTest {

    @Autowired
    private AccessProfileRepository accessProfileRepository;
    @Autowired
    private ZoneRepository zoneRepository;

    private Zone zone;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        zone = Zone.builder()
                .name("Zone A")
                .building("B1")
                .floor("1")
                .sensitive(false)
                .build();
        zone.setCreatedAt(now);
        zone.setUpdatedAt(now);
        zone = zoneRepository.saveAndFlush(zone);
    }

    @Test
    @DisplayName("persists profile with zones and uniqueness")
    void saveAndExists() {
        AccessProfile profile = newProfile("Office", SecurityLevel.MEDIUM, zone);
        AccessProfile saved = accessProfileRepository.saveAndFlush(profile);

        assertThat(saved.getId()).isNotNull();
        assertThat(accessProfileRepository.existsByNameIgnoreCase("office")).isTrue();
        assertThat(accessProfileRepository.hasEmployees(saved.getId())).isFalse();
        assertThat(saved.getZones()).hasSize(1);
    }

    @Test
    @DisplayName("specification filters by security level and zone")
    void searchSpecification() {
        Zone other = Zone.builder().name("Zone B").building("B2").floor("2").sensitive(true).build();
        other.setCreatedAt(Instant.now());
        other.setUpdatedAt(Instant.now());
        other = zoneRepository.saveAndFlush(other);

        accessProfileRepository.saveAndFlush(newProfile("Medium", SecurityLevel.MEDIUM, zone));
        accessProfileRepository.saveAndFlush(newProfile("High", SecurityLevel.HIGH, other));

        AccessProfileSearchCriteria criteria = new AccessProfileSearchCriteria(
                null, SecurityLevel.MEDIUM, zone.getId(), null
        );
        Page<AccessProfile> page = accessProfileRepository.findAll(
                AccessProfileSpecification.withCriteria(criteria),
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getName()).isEqualTo("Medium");
    }

    private AccessProfile newProfile(String name, SecurityLevel level, Zone assignedZone) {
        Instant now = Instant.now();
        AccessProfile profile = AccessProfile.builder()
                .name(name)
                .description(name)
                .securityLevel(level)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .zones(new HashSet<>(Set.of(assignedZone)))
                .build();
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        return profile;
    }
}
