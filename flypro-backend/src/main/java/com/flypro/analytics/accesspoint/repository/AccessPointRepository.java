package com.flypro.analytics.accesspoint.repository;

import com.flypro.analytics.accesspoint.entity.AccessPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for {@link AccessPoint}.
 */
public interface AccessPointRepository extends JpaRepository<AccessPoint, Long>,
        JpaSpecificationExecutor<AccessPoint> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<AccessPoint> findByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<AccessPoint> findByZoneIdOrderByNameAsc(Long zoneId);

    @Query("""
            SELECT CASE WHEN COUNT(ae) > 0 THEN TRUE ELSE FALSE END
            FROM AccessEvent ae
            WHERE ae.accessPoint.id = :accessPointId
            """)
    boolean hasAccessEvents(@Param("accessPointId") Long accessPointId);
}
