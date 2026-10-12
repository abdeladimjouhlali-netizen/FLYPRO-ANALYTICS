package com.flypro.analytics.dashboard.controller;

import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.common.response.ApiResponse;
import com.flypro.analytics.dashboard.dto.ChartDTO;
import com.flypro.analytics.dashboard.dto.DashboardFilterCriteria;
import com.flypro.analytics.dashboard.dto.KpiDTO;
import com.flypro.analytics.dashboard.dto.NamedCountDTO;
import com.flypro.analytics.dashboard.dto.StatisticsDTO;
import com.flypro.analytics.dashboard.dto.TrendDTO;
import com.flypro.analytics.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Read-only REST API for dashboard KPIs, charts and analytics rankings.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Analytics dashboard KPIs, charts, statistics and trends")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(
            summary = "Get dashboard KPIs",
            description = "Returns inventory and activity KPIs. Event/alert KPIs honor date and entity filters."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "KPIs retrieved",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Dashboard KPIs retrieved successfully",
                                      "data": [
                                        {"key":"totalEmployees","label":"Total Employees","value":150,"unit":"count"},
                                        {"key":"grantedAccesses","label":"Granted Accesses","value":980,"unit":"count"}
                                      ]
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<ApiResponse<List<KpiDTO>>> getKpis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long accessPointId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long badgeId,
            @RequestParam(required = false) AlertSeverity alertSeverity,
            @RequestParam(required = false) AlertType alertType
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Dashboard KPIs retrieved successfully",
                dashboardService.getKpis(toFilter(from, to, departmentId, zoneId, accessPointId,
                        employeeId, badgeId, alertSeverity, alertType))
        ));
    }

    @GetMapping("/charts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(
            summary = "Get dashboard charts",
            description = "Daily/weekly/monthly access series plus distribution charts for departments, zones, badges, employees and alerts."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Charts retrieved",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Dashboard charts retrieved successfully",
                                      "data": [
                                        {
                                          "key": "dailyAccessEvents",
                                          "title": "Daily Access Events",
                                          "labels": ["2026-07-20","2026-07-21"],
                                          "series": [{"name":"count","data":[12,18]}]
                                        }
                                      ]
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<ApiResponse<List<ChartDTO>>> getCharts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long accessPointId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long badgeId,
            @RequestParam(required = false) AlertSeverity alertSeverity,
            @RequestParam(required = false) AlertType alertType
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Dashboard charts retrieved successfully",
                dashboardService.getCharts(toFilter(from, to, departmentId, zoneId, accessPointId,
                        employeeId, badgeId, alertSeverity, alertType))
        ));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(
            summary = "Get dashboard statistics",
            description = "Top rankings: access points, zones, employees, denied accesses, expiring badges, departments and alert types."
    )
    public ResponseEntity<ApiResponse<StatisticsDTO>> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long accessPointId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long badgeId,
            @RequestParam(required = false) AlertSeverity alertSeverity,
            @RequestParam(required = false) AlertType alertType
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Dashboard statistics retrieved successfully",
                dashboardService.getStatistics(toFilter(from, to, departmentId, zoneId, accessPointId,
                        employeeId, badgeId, alertSeverity, alertType))
        ));
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(
            summary = "Get dashboard trends",
            description = "Compares the selected period to the previous period of equal length (defaults to last 7 days)."
    )
    public ResponseEntity<ApiResponse<List<TrendDTO>>> getTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long accessPointId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long badgeId,
            @RequestParam(required = false) AlertSeverity alertSeverity,
            @RequestParam(required = false) AlertType alertType
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Dashboard trends retrieved successfully",
                dashboardService.getTrends(toFilter(from, to, departmentId, zoneId, accessPointId,
                        employeeId, badgeId, alertSeverity, alertType))
        ));
    }

    @GetMapping("/top-access-points")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Top busiest access points", description = "Default limit 10, max 100")
    public ResponseEntity<ApiResponse<List<NamedCountDTO>>> getTopAccessPoints(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long accessPointId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long badgeId,
            @RequestParam(required = false) AlertSeverity alertSeverity,
            @RequestParam(required = false) AlertType alertType,
            @Parameter(description = "Max rows (default 10)")
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Top access points retrieved successfully",
                dashboardService.getTopAccessPoints(
                        toFilter(from, to, departmentId, zoneId, accessPointId, employeeId, badgeId, alertSeverity, alertType),
                        limit
                )
        ));
    }

    @GetMapping("/top-zones")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Top busiest zones", description = "Default limit 10, max 100")
    public ResponseEntity<ApiResponse<List<NamedCountDTO>>> getTopZones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long accessPointId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long badgeId,
            @RequestParam(required = false) AlertSeverity alertSeverity,
            @RequestParam(required = false) AlertType alertType,
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Top zones retrieved successfully",
                dashboardService.getTopZones(
                        toFilter(from, to, departmentId, zoneId, accessPointId, employeeId, badgeId, alertSeverity, alertType),
                        limit
                )
        ));
    }

    @GetMapping("/top-employees")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Most active employees", description = "Default limit 10, max 100")
    public ResponseEntity<ApiResponse<List<NamedCountDTO>>> getTopEmployees(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long accessPointId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long badgeId,
            @RequestParam(required = false) AlertSeverity alertSeverity,
            @RequestParam(required = false) AlertType alertType,
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Top employees retrieved successfully",
                dashboardService.getTopEmployees(
                        toFilter(from, to, departmentId, zoneId, accessPointId, employeeId, badgeId, alertSeverity, alertType),
                        limit
                )
        ));
    }

    @GetMapping("/peak-hours")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(
            summary = "Peak access hours",
            description = "Returns 24 buckets (00:00–23:00 UTC) with access event counts."
    )
    public ResponseEntity<ApiResponse<List<NamedCountDTO>>> getPeakHours(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long accessPointId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long badgeId,
            @RequestParam(required = false) AlertSeverity alertSeverity,
            @RequestParam(required = false) AlertType alertType
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Peak hours retrieved successfully",
                dashboardService.getPeakHours(toFilter(from, to, departmentId, zoneId, accessPointId,
                        employeeId, badgeId, alertSeverity, alertType))
        ));
    }

    private static DashboardFilterCriteria toFilter(
            Instant from,
            Instant to,
            Long departmentId,
            Long zoneId,
            Long accessPointId,
            Long employeeId,
            Long badgeId,
            AlertSeverity alertSeverity,
            AlertType alertType
    ) {
        return new DashboardFilterCriteria(
                from, to, departmentId, zoneId, accessPointId, employeeId, badgeId, alertSeverity, alertType
        );
    }
}
