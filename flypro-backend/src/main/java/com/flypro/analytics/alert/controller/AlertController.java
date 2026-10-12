package com.flypro.analytics.alert.controller;

import com.flypro.analytics.alert.dto.AlertResponseDTO;
import com.flypro.analytics.alert.dto.AlertSearchCriteria;
import com.flypro.analytics.alert.service.AlertService;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * REST API for alert search and lifecycle management.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Alerts", description = "Business-rule driven operational alerts")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Search alerts", description = "Paginated search with filters on status, severity, type, employee, badge, zone, department and date range")
    public ResponseEntity<ApiResponse<PageResponse<AlertResponseDTO>>> search(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long badgeId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @Parameter(description = "Keyword on title/description")
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        AlertSearchCriteria criteria = new AlertSearchCriteria(
                status, severity, type, employeeId, badgeId, zoneId, departmentId,
                createdFrom, createdTo, keyword
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Alerts retrieved successfully",
                alertService.search(criteria, pageable)
        ));
    }

    @GetMapping("/alerts/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Get alert by id")
    public ResponseEntity<ApiResponse<AlertResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getById(id)));
    }

    @PatchMapping("/alerts/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Acknowledge alert", description = "Transition OPEN → ACKNOWLEDGED")
    public ResponseEntity<ApiResponse<AlertResponseDTO>> acknowledge(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Alert acknowledged successfully",
                alertService.acknowledge(id)
        ));
    }

    @PatchMapping("/alerts/{id}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Resolve alert",
            description = "Transition OPEN|ACKNOWLEDGED → RESOLVED",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Alert resolved",
                            content = @Content(
                                    schema = @Schema(implementation = AlertResponseDTO.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "success": true,
                                              "message": "Alert resolved successfully",
                                              "data": {
                                                "id": 1,
                                                "status": "RESOLVED",
                                                "type": "ACCESS_DENIED",
                                                "severity": "MEDIUM"
                                              }
                                            }
                                            """)
                            )
                    )
            }
    )
    public ResponseEntity<ApiResponse<AlertResponseDTO>> resolve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Alert resolved successfully",
                alertService.resolve(id)
        ));
    }

    @PatchMapping("/alerts/{id}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Close alert", description = "Transition any non-CLOSED status → CLOSED")
    public ResponseEntity<ApiResponse<AlertResponseDTO>> close(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Alert closed successfully",
                alertService.close(id)
        ));
    }

    @GetMapping("/employees/{employeeId}/alerts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "List alerts by employee")
    public ResponseEntity<ApiResponse<List<AlertResponseDTO>>> byEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Employee alerts retrieved successfully",
                alertService.findByEmployee(employeeId)
        ));
    }

    @GetMapping("/badges/{badgeId}/alerts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "List alerts by badge")
    public ResponseEntity<ApiResponse<List<AlertResponseDTO>>> byBadge(@PathVariable Long badgeId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Badge alerts retrieved successfully",
                alertService.findByBadge(badgeId)
        ));
    }
}
