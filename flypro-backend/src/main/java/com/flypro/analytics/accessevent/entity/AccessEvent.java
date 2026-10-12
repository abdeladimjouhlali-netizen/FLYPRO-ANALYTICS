package com.flypro.analytics.accessevent.entity;

import com.flypro.analytics.accesspoint.entity.AccessPoint;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.common.enums.AccessDirection;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.EventSource;
import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import com.flypro.analytics.employee.entity.Employee;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Access control event captured from a reader / import / API source.
 * Access events are immutable audit records and must never be deleted.
 */
@Entity
@Table(
        name = "access_events",
        indexes = {
                @Index(name = "idx_access_events_event_timestamp", columnList = "event_timestamp"),
                @Index(name = "idx_access_events_badge_id", columnList = "badge_id"),
                @Index(name = "idx_access_events_employee_id", columnList = "employee_id"),
                @Index(name = "idx_access_events_access_point_id", columnList = "access_point_id"),
                @Index(name = "idx_access_events_zone_id", columnList = "zone_id"),
                @Index(name = "idx_access_events_result", columnList = "result"),
                @Index(name = "idx_access_events_source", columnList = "source")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccessEvent extends BaseAuditableEntity {

    @NotNull
    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private AccessDirection direction;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 30)
    private AccessResult result;

    @Size(max = 500)
    @Column(name = "refusal_reason", length = 500)
    private String refusalReason;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private EventSource source;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "access_point_id", nullable = false)
    private AccessPoint accessPoint;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;
}
