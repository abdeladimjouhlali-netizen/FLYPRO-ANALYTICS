package com.flypro.analytics.accessevent.repository;

import com.flypro.analytics.common.test.FlyproDataJpaTest;
import com.flypro.analytics.accessevent.dto.AccessEventSearchCriteria;
import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.accessevent.specification.AccessEventSpecification;
import com.flypro.analytics.accesspoint.entity.AccessPoint;
import com.flypro.analytics.accesspoint.repository.AccessPointRepository;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.badge.repository.BadgeRepository;
import com.flypro.analytics.common.enums.AccessDirection;
import com.flypro.analytics.common.enums.AccessPointStatus;
import com.flypro.analytics.common.enums.AccessPointType;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.BadgeType;
import com.flypro.analytics.common.enums.EventSource;
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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@FlyproDataJpaTest
class AccessEventRepositoryTest {

    @Autowired
    private AccessEventRepository accessEventRepository;
    @Autowired
    private BadgeRepository badgeRepository;
    @Autowired
    private AccessPointRepository accessPointRepository;
    @Autowired
    private ZoneRepository zoneRepository;

    private Badge badge;
    private AccessPoint accessPoint;
    private Zone zone;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        zone = Zone.builder().name("Lobby").building("North").floor("0").sensitive(false).build();
        zone.setCreatedAt(now);
        zone.setUpdatedAt(now);
        zone = zoneRepository.saveAndFlush(zone);

        accessPoint = AccessPoint.builder()
                .name("Main Door").code("AP-01").type(AccessPointType.DOOR)
                .status(AccessPointStatus.ACTIVE).zone(zone).build();
        accessPoint.setCreatedAt(now);
        accessPoint.setUpdatedAt(now);
        accessPoint = accessPointRepository.saveAndFlush(accessPoint);

        badge = Badge.builder()
                .badgeNumber("BDG-REPO-1").type(BadgeType.RFID).status(BadgeStatus.ACTIVE)
                .issueDate(LocalDate.now().minusDays(10))
                .expirationDate(LocalDate.now().plusYears(1))
                .build();
        badge.setCreatedAt(now);
        badge.setUpdatedAt(now);
        badge = badgeRepository.saveAndFlush(badge);
    }

    @Test
    @DisplayName("persists access event")
    void save() {
        AccessEvent event = newEvent(Instant.parse("2026-07-20T10:00:00Z"), AccessResult.AUTHORIZED);
        AccessEvent saved = accessEventRepository.saveAndFlush(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getBadge().getBadgeNumber()).isEqualTo("BDG-REPO-1");
    }

    @Test
    @DisplayName("specification filters by result, source and date range")
    void searchSpecification() {
        accessEventRepository.save(newEvent(Instant.parse("2026-07-10T10:00:00Z"), AccessResult.AUTHORIZED));
        accessEventRepository.save(newEvent(Instant.parse("2026-07-25T10:00:00Z"), AccessResult.DENIED));
        accessEventRepository.flush();

        AccessEventSearchCriteria criteria = new AccessEventSearchCriteria(
                "BDG-REPO", null, null, zone.getId(), accessPoint.getId(),
                AccessResult.AUTHORIZED, AccessDirection.ENTRY, EventSource.API,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-20T23:59:59Z"),
                null
        );

        Page<AccessEvent> page = accessEventRepository.findAll(
                AccessEventSpecification.withCriteria(criteria),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "eventTimestamp"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getResult()).isEqualTo(AccessResult.AUTHORIZED);
    }

    private AccessEvent newEvent(Instant timestamp, AccessResult result) {
        Instant now = Instant.now();
        AccessEvent event = AccessEvent.builder()
                .eventTimestamp(timestamp)
                .direction(AccessDirection.ENTRY)
                .result(result)
                .source(EventSource.API)
                .rawPayload("{}")
                .badge(badge)
                .accessPoint(accessPoint)
                .zone(zone)
                .build();
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        return event;
    }
}
