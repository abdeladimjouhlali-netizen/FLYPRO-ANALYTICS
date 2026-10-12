package com.flypro.analytics.report.entity;

import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import com.flypro.analytics.report.enums.ExportFormat;
import com.flypro.analytics.report.enums.ReportFrequency;
import com.flypro.analytics.report.enums.ReportKind;
import com.flypro.analytics.report.enums.ReportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Recurring report schedule (table created by {@code V11__reports_notifications.sql}).
 */
@Entity
@Table(name = "report_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReportSchedule extends BaseAuditableEntity {

    @NotBlank
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 50)
    private ReportType reportType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "export_format", nullable = false, length = 20)
    private ExportFormat exportFormat;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "report_kind", nullable = false, length = 30)
    private ReportKind reportKind;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private ReportFrequency frequency;

    @Column(name = "filters_json", columnDefinition = "TEXT")
    private String filtersJson;

    @NotBlank
    @Column(name = "recipients", nullable = false, length = 1000)
    private String recipients;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;
}
