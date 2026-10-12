package com.flypro.analytics.report.dto;

import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.report.enums.ExportFormat;
import com.flypro.analytics.report.enums.ReportKind;
import com.flypro.analytics.report.enums.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Shared filter criteria for report generation and export.
 */
@Schema(description = "Report filter criteria")
public record ReportFilterCriteria(
        Instant from,
        Instant to,
        Long departmentId,
        Long employeeId,
        Long badgeId,
        Long zoneId,
        Long accessPointId,
        AlertSeverity alertSeverity,
        AlertType alertType,
        AccessResult accessResult,
        String username,
        ReportKind reportKind
) {
    public ReportFilterCriteria {
        if (reportKind == null) {
            reportKind = ReportKind.DETAILED;
        }
    }

    @AssertTrue(message = "from must be before or equal to to")
    public boolean isValidDateRange() {
        if (from == null || to == null) {
            return true;
        }
        return !from.isAfter(to);
    }
}
