package com.flypro.analytics.report.service;

import com.flypro.analytics.report.dto.ExportFileResult;
import com.flypro.analytics.report.dto.ExportReportRequest;
import com.flypro.analytics.report.dto.ReportFilterCriteria;
import com.flypro.analytics.report.dto.ReportResponseDTO;
import com.flypro.analytics.report.dto.ReportScheduleResponse;
import com.flypro.analytics.report.dto.ScheduleReportRequest;
import com.flypro.analytics.report.enums.ReportType;

/**
 * Reporting façade.
 */
public interface ReportService {

    ReportResponseDTO getReport(ReportType type, ReportFilterCriteria filters);

    ExportFileResult export(ExportReportRequest request);

    ReportScheduleResponse schedule(ScheduleReportRequest request);

    void runDueSchedules();
}
