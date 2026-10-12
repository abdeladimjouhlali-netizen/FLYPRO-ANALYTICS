package com.flypro.analytics.report.model;

import com.flypro.analytics.report.enums.ReportKind;
import com.flypro.analytics.report.enums.ReportType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Domain-agnostic tabular report, ready to be handed to a {@link com.flypro.analytics.report.generator.ReportGenerator}.
 */
@Getter
@Builder
public class ReportDocument {

    private final ReportType reportType;
    private final ReportKind reportKind;
    private final String title;
    private final Instant generatedAt;
    private final Map<String, Object> summary;
    private final List<String> columns;
    private final List<Map<String, Object>> rows;
}
