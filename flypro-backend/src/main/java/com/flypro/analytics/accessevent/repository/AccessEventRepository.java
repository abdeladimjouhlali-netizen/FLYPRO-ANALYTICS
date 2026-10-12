package com.flypro.analytics.accessevent.repository;

import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.common.enums.AccessDirection;
import com.flypro.analytics.common.enums.AccessResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Collection;

/**
 * Persistence port for {@link AccessEvent}.
 * Events are append-only: no delete operations are exposed by the service layer.
 */
public interface AccessEventRepository extends JpaRepository<AccessEvent, Long>,
        JpaSpecificationExecutor<AccessEvent> {

    boolean existsByBadgeIdAndAccessPointIdAndEventTimestampAndDirection(
            Long badgeId,
            Long accessPointId,
            Instant eventTimestamp,
            AccessDirection direction
    );

    long countByBadgeIdAndResultInAndEventTimestampGreaterThanEqual(
            Long badgeId,
            Collection<AccessResult> results,
            Instant since
    );
}
