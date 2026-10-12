package com.flypro.analytics.accessprofile.repository;

import com.flypro.analytics.accessprofile.entity.AccessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence port for {@link AccessProfile}.
 */
public interface AccessProfileRepository extends JpaRepository<AccessProfile, Long>,
        JpaSpecificationExecutor<AccessProfile> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN TRUE ELSE FALSE END
            FROM Employee e
            WHERE e.accessProfile.id = :profileId
            """)
    boolean hasEmployees(@Param("profileId") Long profileId);

    @Query("""
            SELECT DISTINCT p FROM AccessProfile p
            LEFT JOIN FETCH p.zones
            WHERE p.id = :id
            """)
    java.util.Optional<AccessProfile> findByIdWithZones(@Param("id") Long id);
}
