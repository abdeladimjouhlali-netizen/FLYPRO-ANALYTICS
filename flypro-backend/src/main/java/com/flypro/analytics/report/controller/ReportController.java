package com.flypro.analytics.report.controller;

import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.common.response.ApiResponse;
import com.flypro.analytics.report.dto.ExportFileResult;
import com.flypro.analytics.report.dto.ExportReportRequest;
import com.flypro.analytics.report.dto.ReportFilterCriteria;
import com.flypro.analytics.report.dto.ReportResponseDTO;
import com.flypro.analytics.report.dto.ReportScheduleResponse;
import com.flypro.analytics.report.dto.ScheduleReportRequest;
import com.flypro.analytics.report.enums.ReportKind;
import com.flypro.analytics.report.enums.ReportType;
import com.flypro.analytics.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Reporting & export REST API (Phase 5 – Step 19).
 */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Generate, export and schedule analytics reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Employees report (JSON)")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> employeesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) ReportKind reportKind
    ) {
        return ok(ReportType.EMPLOYEES, filters(from, to, departmentId, employeeId, null, null, null,
                null, null, null, null, reportKind));
    }

    @GetMapping("/access-events")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Access events report (JSON)")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> accessEventsReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long badgeId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long accessPointId,
            @RequestParam(required = false) AccessResult accessResult,
            @RequestParam(required = false) ReportKind reportKind
    ) {
        return ok(ReportType.ACCESS_EVENTS, filters(from, to, departmentId, employeeId, badgeId, zoneId,
                accessPointId, null, null, accessResult, null, reportKind));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Alerts report (JSON)")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> alertsReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long badgeId,
            @RequestParam(required = false) AlertSeverity alertSeverity,
            @RequestParam(required = false) AlertType alertType,
            @RequestParam(required = false) ReportKind reportKind
    ) {
        return ok(ReportType.ALERTS, filters(from, to, null, employeeId, badgeId, null, null,
                alertSeverity, alertType, null, null, reportKind));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Dashboard statistics report (JSON)")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> dashboardReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) ReportKind reportKind
    ) {
        return ok(ReportType.DASHBOARD_STATISTICS, filters(from, to, departmentId, null, null, zoneId,
                null, null, null, null, null, reportKind != null ? reportKind : ReportKind.STATISTICS));
    }

    @PostMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Export a report file (PDF / Excel / CSV / JSON)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = ExportReportRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "reportType": "ACCESS_EVENTS",
                                      "exportFormat": "PDF",
                                      "reportKind": "DETAILED",
                                      "filters": {
                                        "from": "2026-01-01T00:00:00Z",
                                        "to": "2026-07-27T23:59:59Z"
                                      },
                                      "notifyEmails": ["admin@flypro.local"]
                                    }
                                    """)
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Binary export",
                    content = @Content(mediaType = "application/octet-stream")
            )
    })
    public ResponseEntity<byte[]> export(@Valid @RequestBody ExportReportRequest request) {
        ExportFileResult file = reportService.export(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }

    @PostMapping("/schedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Schedule a recurring report",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "Weekly access events",
                                      "reportType": "ACCESS_EVENTS",
                                      "exportFormat": "EXCEL",
                                      "reportKind": "SUMMARY",
                                      "frequency": "WEEKLY",
                                      "filters": {},
                                      "recipients": ["admin@flypro.local"]
                                    }
                                    """)
                    )
            )
    )
    public ResponseEntity<ApiResponse<ReportScheduleResponse>> schedule(
            @Valid @RequestBody ScheduleReportRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Report scheduled successfully", reportService.schedule(request)));
    }

    private ResponseEntity<ApiResponse<ReportResponseDTO>> ok(ReportType type, ReportFilterCriteria filters) {
        return ResponseEntity.ok(ApiResponse.ok("Report generated successfully",
                reportService.getReport(type, filters)));
    }

    private static ReportFilterCriteria filters(Instant from, Instant to, Long departmentId, Long employeeId,
                                                Long badgeId, Long zoneId, Long accessPointId,
                                                AlertSeverity alertSeverity, AlertType alertType,
                                                AccessResult accessResult, String username,
                                                ReportKind reportKind) {
        return new ReportFilterCriteria(from, to, departmentId, employeeId, badgeId, zoneId, accessPointId,
                alertSeverity, alertType, accessResult, username,
                reportKind != null ? reportKind : ReportKind.DETAILED);
    }
}
