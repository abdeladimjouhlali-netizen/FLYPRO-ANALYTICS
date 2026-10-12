package com.flypro.analytics.report.service;

import com.flypro.analytics.report.dto.ReportFilterCriteria;
import com.flypro.analytics.report.enums.ReportType;
import com.flypro.analytics.report.model.ReportDocument;

/**
 * Builds {@link ReportDocument} instances from domain data (read-only).
 */
public interface ReportDocumentFactory {

    ReportDocument build(ReportType type, ReportFilterCriteria filters);
}
