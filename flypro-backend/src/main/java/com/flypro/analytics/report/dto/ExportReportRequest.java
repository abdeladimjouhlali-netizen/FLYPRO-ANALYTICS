package com.flypro.analytics.report.dto;

import com.flypro.analytics.report.enums.ExportFormat;
import com.flypro.analytics.report.enums.ReportKind;
import com.flypro.analytics.report.enums.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request payload for {@code POST /api/v1/reports/export}.
 */
@Schema(description = "Report export request")
public record ExportReportRequest(

        @NotNull
        @Schema(example = "ACCESS_EVENTS")
        ReportType reportType,

        @NotNull
        @Schema(example = "PDF")
        ExportFormat exportFormat,

        @Schema(example = "DETAILED")
        ReportKind reportKind,

        @Valid
        ReportFilterCriteria filters,

        List<String> notifyEmails
) {
    public ExportReportRequest {
        if (reportKind == null) {
            reportKind = ReportKind.DETAILED;
        }
        if (filters == null) {
            filters = new ReportFilterCriteria(null, null, null, null, null, null, null, null, null, null, null, reportKind);
        }
    }
}
