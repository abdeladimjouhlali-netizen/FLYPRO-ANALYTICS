package com.flypro.analytics.report.dto;

import com.flypro.analytics.report.enums.ExportFormat;
import com.flypro.analytics.report.enums.ReportFrequency;
import com.flypro.analytics.report.enums.ReportKind;
import com.flypro.analytics.report.enums.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request payload for {@code POST /api/v1/reports/schedule}.
 */
@Schema(description = "Recurring report schedule request")
public record ScheduleReportRequest(

        @NotBlank
        @Schema(example = "Weekly access events")
        String name,

        @NotNull
        @Schema(example = "ACCESS_EVENTS")
        ReportType reportType,

        @NotNull
        @Schema(example = "EXCEL")
        ExportFormat exportFormat,

        @Schema(example = "SUMMARY")
        ReportKind reportKind,

        @NotNull
        @Schema(example = "WEEKLY")
        ReportFrequency frequency,

        @Valid
        ReportFilterCriteria filters,

        @NotEmpty
        List<@Email String> recipients
) {
    public ScheduleReportRequest {
        if (reportKind == null) {
            reportKind = ReportKind.SUMMARY;
        }
        if (filters == null) {
            filters = new ReportFilterCriteria(null, null, null, null, null, null, null, null, null, null, null, reportKind);
        }
    }
}
