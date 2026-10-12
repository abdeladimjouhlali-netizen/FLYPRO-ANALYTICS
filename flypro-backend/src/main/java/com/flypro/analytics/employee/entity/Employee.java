package com.flypro.analytics.employee.entity;

import com.flypro.analytics.accessprofile.entity.AccessProfile;
import com.flypro.analytics.badgeassignment.entity.BadgeAssignment;
import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import com.flypro.analytics.department.entity.Department;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Employee belonging to a department and optionally linked to an access profile.
 */
@Entity
@Table(
        name = "employees",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employees_employee_number", columnNames = "employee_number"),
                @UniqueConstraint(name = "uk_employees_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_employees_employee_number", columnList = "employee_number"),
                @Index(name = "idx_employees_email", columnList = "email"),
                @Index(name = "idx_employees_department_id", columnList = "department_id"),
                @Index(name = "idx_employees_access_profile_id", columnList = "access_profile_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Employee extends BaseAuditableEntity {

    @NotBlank
    @Size(max = 50)
    @Column(name = "employee_number", nullable = false, length = 50)
    private String employeeNumber;

    @NotBlank
    @Size(max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Size(max = 30)
    @Pattern(regexp = "^[+0-9()\\-\\s]*$", message = "Invalid phone number format")
    @Column(name = "phone", length = 30)
    private String phone;

    @NotBlank
    @Size(max = 150)
    @Column(name = "position", nullable = false, length = 150)
    private String position;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EmployeeStatus status;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_profile_id")
    private AccessProfile accessProfile;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<BadgeAssignment> badgeAssignments = new ArrayList<>();
}
