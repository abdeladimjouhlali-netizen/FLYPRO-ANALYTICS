package com.flypro.analytics.department.entity;

import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import com.flypro.analytics.employee.entity.Employee;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
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
 * Organizational department.
 */
@Entity
@Table(
        name = "departments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_departments_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_departments_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "idx_departments_code", columnList = "code"),
                @Index(name = "idx_departments_name", columnList = "name"),
                @Index(name = "idx_departments_active", columnList = "active"),
                @Index(name = "idx_departments_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Department extends BaseAuditableEntity {

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotBlank
    @Size(max = 50)
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Soft-delete flag. {@code false} means the department is deactivated.
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<Employee> employees = new ArrayList<>();
}
