package com.flypro.analytics.badgeassignment.repository;

import com.flypro.analytics.badgeassignment.entity.BadgeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for {@link BadgeAssignment}. Assignment history is never deleted.
 */
public interface BadgeAssignmentRepository extends JpaRepository<BadgeAssignment, Long> {

    Optional<BadgeAssignment> findByBadgeIdAndActiveTrue(Long badgeId);

    Optional<BadgeAssignment> findByEmployeeIdAndActiveTrue(Long employeeId);

    boolean existsByBadgeIdAndActiveTrue(Long badgeId);

    boolean existsByEmployeeIdAndActiveTrue(Long employeeId);

    List<BadgeAssignment> findByBadgeIdOrderByAssignedAtDesc(Long badgeId);

    List<BadgeAssignment> findByEmployeeIdOrderByAssignedAtDesc(Long employeeId);

    @Query("""
            SELECT ba FROM BadgeAssignment ba
            JOIN FETCH ba.badge
            JOIN FETCH ba.employee e
            JOIN FETCH e.department
            WHERE ba.employee.id = :employeeId
            ORDER BY ba.assignedAt DESC
            """)
    List<BadgeAssignment> findAllByEmployeeIdWithDetails(@Param("employeeId") Long employeeId);

    @Query("""
            SELECT ba FROM BadgeAssignment ba
            JOIN FETCH ba.employee e
            JOIN FETCH e.department
            WHERE ba.badge.id = :badgeId
            ORDER BY ba.assignedAt DESC
            """)
    List<BadgeAssignment> findHistoryByBadgeId(@Param("badgeId") Long badgeId);
}
