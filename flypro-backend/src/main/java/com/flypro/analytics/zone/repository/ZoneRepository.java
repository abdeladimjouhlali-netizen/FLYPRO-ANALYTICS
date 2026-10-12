package com.flypro.analytics.zone.repository;

import com.flypro.analytics.zone.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Persistence port for {@link Zone}.
 */
public interface ZoneRepository extends JpaRepository<Zone, Long>, JpaSpecificationExecutor<Zone> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Zone> findByIdIn(Collection<Long> ids);

    @Query("""
            SELECT CASE WHEN COUNT(ap) > 0 THEN TRUE ELSE FALSE END
            FROM AccessPoint ap
            WHERE ap.zone.id = :zoneId
            """)
    boolean hasAccessPoints(@Param("zoneId") Long zoneId);

    @Query(value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
            FROM access_profile_zones apz
            WHERE apz.zone_id = :zoneId
            """, nativeQuery = true)
    boolean isAssignedToAnyProfile(@Param("zoneId") Long zoneId);
}
