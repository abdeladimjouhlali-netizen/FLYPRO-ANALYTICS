package com.flypro.analytics.report.generator;

import com.flypro.analytics.report.enums.ExportFormat;
import com.flypro.analytics.report.model.ReportDocument;

/**
 * Strategy contract for export generators.
 */
public interface ReportGenerator {

    ExportFormat format();

    /**
     * @return binary payload (UTF-8 for CSV/JSON)
     */
    byte[] generate(ReportDocument document);

    default String contentType() {
        return switch (format()) {
            case PDF -> "application/pdf";
            case EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case CSV -> "text/csv";
            case JSON -> "application/json";
        };
    }

    default String fileExtension() {
        return switch (format()) {
            case PDF -> "pdf";
            case EXCEL -> "xlsx";
            case CSV -> "csv";
            case JSON -> "json";
        };
    }
}
