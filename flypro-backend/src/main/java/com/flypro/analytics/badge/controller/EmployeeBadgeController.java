package com.flypro.analytics.badge.controller;

import com.flypro.analytics.badge.dto.BadgeResponseDTO;
import com.flypro.analytics.badge.service.BadgeService;
import com.flypro.analytics.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Employee-scoped badge listing endpoint.
 */
@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employee Badges", description = "List badges linked to an employee")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeBadgeController {

    private final BadgeService badgeService;

    public EmployeeBadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    @GetMapping("/{employeeId}/badges")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "List badges for an employee")
    public ResponseEntity<ApiResponse<List<BadgeResponseDTO>>> listEmployeeBadges(
            @PathVariable Long employeeId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Employee badges retrieved successfully",
                badgeService.listEmployeeBadges(employeeId)
        ));
    }
}
