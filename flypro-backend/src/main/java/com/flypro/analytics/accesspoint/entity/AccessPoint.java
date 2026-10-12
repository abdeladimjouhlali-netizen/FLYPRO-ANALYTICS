package com.flypro.analytics.accesspoint.entity;

import com.flypro.analytics.common.enums.AccessPointStatus;
import com.flypro.analytics.common.enums.AccessPointType;
import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import com.flypro.analytics.zone.entity.Zone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Access control point located in a zone.
 */
@Entity
@Table(
        name = "access_points",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_access_points_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_access_points_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "idx_access_points_code", columnList = "code"),
                @Index(name = "idx_access_points_name", columnList = "name"),
                @Index(name = "idx_access_points_zone_id", columnList = "zone_id"),
                @Index(name = "idx_access_points_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccessPoint extends BaseAuditableEntity {

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotBlank
    @Size(max = 50)
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private AccessPointType type;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AccessPointStatus status;

    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Size(max = 255)
    @Column(name = "location", length = 255)
    private String location;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;
}
