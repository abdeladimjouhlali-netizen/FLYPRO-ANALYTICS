package com.flypro.analytics.alert.repository;

import com.flypro.analytics.alert.entity.Alert;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Persistence port for {@link Alert}.
 * Alerts are never deleted.
 */
public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    List<Alert> findByEmployeeIdOrderByCreatedDateDesc(Long employeeId);

    List<Alert> findByBadgeIdOrderByCreatedDateDesc(Long badgeId);

    List<Alert> findBySeverityOrderByCreatedDateDesc(com.flypro.analytics.common.enums.AlertSeverity severity);

    boolean existsByTypeAndAccessEventId(AlertType type, Long accessEventId);

    boolean existsByTypeAndBadgeIdAndStatusIn(AlertType type, Long badgeId, Collection<AlertStatus> statuses);

    boolean existsByTypeAndEmployeeIdAndStatusIn(AlertType type, Long employeeId, Collection<AlertStatus> statuses);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
            FROM Alert a
            WHERE a.type = :type
              AND a.badge.id = :badgeId
              AND a.status IN :statuses
              AND a.accessEvent.id = :accessEventId
            """)
    boolean existsDuplicateForEvent(@Param("type") AlertType type,
                                    @Param("badgeId") Long badgeId,
                                    @Param("accessEventId") Long accessEventId,
                                    @Param("statuses") Collection<AlertStatus> statuses);
}
