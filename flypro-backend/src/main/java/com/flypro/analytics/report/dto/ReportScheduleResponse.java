package com.flypro.analytics.report.dto;

import com.flypro.analytics.report.enums.ExportFormat;
import com.flypro.analytics.report.enums.ReportFrequency;
import com.flypro.analytics.report.enums.ReportKind;
import com.flypro.analytics.report.enums.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Response returned after creating/reading a recurring report schedule.
 */
@Schema(description = "Recurring report schedule")
public record ReportScheduleResponse(
        Long id,
        String name,
        ReportType reportType,
        ExportFormat exportFormat,
        ReportKind reportKind,
        ReportFrequency frequency,
        List<String> recipients,
        boolean enabled,
        Instant lastRunAt,
        Instant nextRunAt,
        Instant createdAt
) {
}
