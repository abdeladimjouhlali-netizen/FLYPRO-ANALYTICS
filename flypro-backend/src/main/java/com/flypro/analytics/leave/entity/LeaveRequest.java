package com.flypro.analytics.leave.entity;

import com.flypro.analytics.common.enums.LeaveStatus;
import com.flypro.analytics.common.enums.LeaveType;
import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import com.flypro.analytics.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Leave request submitted by an employee. While an APPROVED request covers
 * the current date, the employee's badge access is denied.
 */
@Entity
@Table(
        name = "leave_requests",
        indexes = {
                @Index(name = "idx_leave_requests_employee_id", columnList = "employee_id"),
                @Index(name = "idx_leave_requests_status", columnList = "status"),
                @Index(name = "idx_leave_requests_dates", columnList = "start_date, end_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LeaveRequest extends BaseAuditableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private LeaveType type;

    @Column(name = "reason", length = 1000)
    private String reason;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LeaveStatus status;

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;
}
