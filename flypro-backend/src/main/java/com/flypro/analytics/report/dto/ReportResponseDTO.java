package com.flypro.analytics.report.dto;

import com.flypro.analytics.report.enums.ReportKind;
import com.flypro.analytics.report.enums.ReportType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * JSON report payload returned by GET report endpoints.
 */
public record ReportResponseDTO(
        ReportType reportType,
        ReportKind reportKind,
        String title,
        Instant generatedAt,
        Map<String, Object> summary,
        List<String> columns,
        List<Map<String, Object>> rows,
        long totalRows
) {
}
