package com.flypro.analytics.accesspoint.controller;

import com.flypro.analytics.accesspoint.dto.AccessPointResponseDTO;
import com.flypro.analytics.accesspoint.service.AccessPointService;
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
 * Zone-scoped access point listing endpoint.
 */
@RestController
@RequestMapping("/api/v1/zones")
@Tag(name = "Zone Access Points", description = "List access points belonging to a zone")
@SecurityRequirement(name = "bearerAuth")
public class ZoneAccessPointController {

    private final AccessPointService accessPointService;

    public ZoneAccessPointController(AccessPointService accessPointService) {
        this.accessPointService = accessPointService;
    }

    @GetMapping("/{zoneId}/access-points")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "List access points for a zone")
    public ResponseEntity<ApiResponse<List<AccessPointResponseDTO>>> listByZone(
            @PathVariable Long zoneId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Zone access points retrieved successfully",
                accessPointService.listByZone(zoneId)
        ));
    }
}
