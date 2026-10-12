package com.flypro.analytics.badge.repository;

import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.common.enums.BadgeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistence port for {@link Badge}.
 */
public interface BadgeRepository extends JpaRepository<Badge, Long>, JpaSpecificationExecutor<Badge> {

    boolean existsByBadgeNumberIgnoreCase(String badgeNumber);

    boolean existsByBadgeNumberIgnoreCaseAndIdNot(String badgeNumber, Long id);

    Optional<Badge> findByBadgeNumberIgnoreCase(String badgeNumber);

    @Query("""
            SELECT CASE WHEN COUNT(ba) > 0 THEN TRUE ELSE FALSE END
            FROM BadgeAssignment ba
            WHERE ba.badge.id = :badgeId
            """)
    boolean hasAnyAssignment(@Param("badgeId") Long badgeId);

    @Query("""
            SELECT b FROM Badge b
            WHERE b.status <> :expired
              AND b.expirationDate IS NOT NULL
              AND b.expirationDate < :today
            """)
    List<Badge> findExpiredCandidates(@Param("today") LocalDate today,
                                      @Param("expired") BadgeStatus expired);

    default List<Badge> findExpiredCandidates(LocalDate today) {
        return findExpiredCandidates(today, BadgeStatus.EXPIRED);
    }
}
