package com.flypro.analytics.accessprofile.entity;

import com.flypro.analytics.common.enums.SecurityLevel;
import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.zone.entity.Zone;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Access rights profile assigned to employees and authorized for specific zones.
 */
@Entity
@Table(
        name = "access_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_access_profiles_name", columnNames = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccessProfile extends BaseAuditableEntity {

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "security_level", nullable = false, length = 30)
    private SecurityLevel securityLevel;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "access_profile_zones",
            joinColumns = @JoinColumn(name = "access_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "zone_id")
    )
    @Builder.Default
    private Set<Zone> zones = new HashSet<>();

    @OneToMany(mappedBy = "accessProfile", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<Employee> employees = new ArrayList<>();
}
