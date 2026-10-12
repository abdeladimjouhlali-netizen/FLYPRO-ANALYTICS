package com.flypro.analytics.department.repository;

import com.flypro.analytics.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence port for {@link Department}.
 */
public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN TRUE ELSE FALSE END
            FROM Employee e
            WHERE e.department.id = :departmentId
            """)
    boolean hasEmployees(@Param("departmentId") Long departmentId);
}
