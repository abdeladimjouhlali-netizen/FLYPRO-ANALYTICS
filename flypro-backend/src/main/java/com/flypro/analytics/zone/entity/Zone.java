package com.flypro.analytics.zone.entity;

import com.flypro.analytics.accesspoint.entity.AccessPoint;
import com.flypro.analytics.accessprofile.entity.AccessProfile;
import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Physical or logical access zone.
 */
@Entity
@Table(
        name = "zones",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_zones_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "idx_zones_name", columnList = "name"),
                @Index(name = "idx_zones_building", columnList = "building")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Zone extends BaseAuditableEntity {

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotBlank
    @Size(max = 150)
    @Column(name = "building", nullable = false, length = 150)
    private String building;

    @NotBlank
    @Size(max = 50)
    @Column(name = "floor", nullable = false, length = 50)
    private String floor;

    @Column(name = "is_sensitive", nullable = false)
    private boolean sensitive;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<AccessPoint> accessPoints = new ArrayList<>();

    @ManyToMany(mappedBy = "zones")
    @Builder.Default
    private Set<AccessProfile> accessProfiles = new HashSet<>();
}
