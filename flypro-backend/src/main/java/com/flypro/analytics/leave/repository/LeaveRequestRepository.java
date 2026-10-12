package com.flypro.analytics.leave.repository;

import com.flypro.analytics.common.enums.LeaveStatus;
import com.flypro.analytics.leave.entity.LeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;

/**
 * Persistence port for {@link LeaveRequest}.
 */
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>,
        JpaSpecificationExecutor<LeaveRequest> {

    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    boolean existsByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId,
            LeaveStatus status,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("""
            select (count(l) > 0) from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status in :statuses
              and l.startDate <= :endDate
              and l.endDate >= :startDate
            """)
    boolean existsOverlapping(
            @Param("employeeId") Long employeeId,
            @Param("statuses") Collection<LeaveStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select (count(l) > 0) from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status in :statuses
              and l.startDate <= :endDate
              and l.endDate >= :startDate
              and l.id <> :excludedId
            """)
    boolean existsOverlappingExcluding(
            @Param("employeeId") Long employeeId,
            @Param("statuses") Collection<LeaveStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludedId") Long excludedId
    );
}
