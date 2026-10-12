package com.flypro.analytics.badgeassignment.entity;

import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import com.flypro.analytics.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Assignment of a badge to an employee over a period of time.
 */
@Entity
@Table(
        name = "badge_assignments",
        indexes = {
                @Index(name = "idx_badge_assignments_badge_id", columnList = "badge_id"),
                @Index(name = "idx_badge_assignments_employee_id", columnList = "employee_id"),
                @Index(name = "idx_badge_assignments_active", columnList = "active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BadgeAssignment extends BaseAuditableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotNull
    @PastOrPresent
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "active", nullable = false)
    private boolean active;
}
