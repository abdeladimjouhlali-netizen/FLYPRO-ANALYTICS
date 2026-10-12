package com.flypro.analytics.alert.entity;

import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.enums.AlertType;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Operational alert related to access anomalies or policy violations.
 * Alerts are never deleted; they transition through lifecycle statuses.
 */
@Entity
@Table(
        name = "alerts",
        indexes = {
                @Index(name = "idx_alerts_status", columnList = "status"),
                @Index(name = "idx_alerts_severity", columnList = "severity"),
                @Index(name = "idx_alerts_type", columnList = "type"),
                @Index(name = "idx_alerts_employee_id", columnList = "employee_id"),
                @Index(name = "idx_alerts_badge_id", columnList = "badge_id"),
                @Index(name = "idx_alerts_access_event_id", columnList = "access_event_id"),
                @Index(name = "idx_alerts_created_date", columnList = "created_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Alert extends BaseAuditableEntity {

    @NotBlank
    @Size(max = 200)
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Size(max = 2000)
    @Column(name = "description", length = 2000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private AlertSeverity severity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AlertStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private AlertType type;

    @NotNull
    @PastOrPresent
    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @Column(name = "resolved_date")
    private Instant resolvedDate;

    @Size(max = 100)
    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id")
    private Badge badge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_event_id")
    private AccessEvent accessEvent;
}
