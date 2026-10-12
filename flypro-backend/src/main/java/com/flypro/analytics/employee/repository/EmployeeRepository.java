package com.flypro.analytics.employee.repository;

import com.flypro.analytics.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence port for {@link Employee}.
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    boolean existsByEmployeeNumberIgnoreCase(String employeeNumber);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmployeeNumberIgnoreCaseAndIdNot(String employeeNumber, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("""
            SELECT COUNT(ba) > 0
            FROM BadgeAssignment ba
            WHERE ba.employee.id = :employeeId AND ba.active = :active
            """)
    boolean hasActiveBadgeAssignments(@Param("employeeId") Long employeeId, @Param("active") boolean active);

    default boolean hasActiveBadgeAssignments(Long employeeId) {
        return hasActiveBadgeAssignments(employeeId, true);
    }
}
