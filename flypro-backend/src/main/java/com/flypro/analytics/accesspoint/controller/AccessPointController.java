package com.flypro.analytics.accesspoint.controller;

import com.flypro.analytics.accesspoint.dto.AccessPointRequestDTO;
import com.flypro.analytics.accesspoint.dto.AccessPointResponseDTO;
import com.flypro.analytics.accesspoint.dto.AccessPointSearchCriteria;
import com.flypro.analytics.accesspoint.service.AccessPointService;
import com.flypro.analytics.common.enums.AccessPointStatus;
import com.flypro.analytics.common.enums.AccessPointType;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for access point management.
 */
@RestController
@RequestMapping("/api/v1/access-points")
@Tag(name = "Access Points", description = "Access point CRUD, search and status transitions")
@SecurityRequirement(name = "bearerAuth")
public class AccessPointController {

    private final AccessPointService accessPointService;

    public AccessPointController(AccessPointService accessPointService) {
        this.accessPointService = accessPointService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Search access points", description = "Paginated search with filters and keyword")
    public ResponseEntity<ApiResponse<PageResponse<AccessPointResponseDTO>>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) AccessPointType type,
            @RequestParam(required = false) AccessPointStatus status,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String floor,
            @Parameter(description = "Global keyword across name, code, description, location, IP and zone")
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        AccessPointSearchCriteria criteria = new AccessPointSearchCriteria(
                name, code, type, status, zoneId, building, floor, keyword
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Access points retrieved successfully",
                accessPointService.search(criteria, pageable)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Get access point by id")
    public ResponseEntity<ApiResponse<AccessPointResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(accessPointService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create access point")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = AccessPointRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Create access point",
                            value = """
                                    {
                                      "name": "Main Entrance Door",
                                      "code": "AP-MAIN-01",
                                      "type": "DOOR",
                                      "description": "Primary entrance door reader",
                                      "status": "ACTIVE",
                                      "ipAddress": "192.168.1.10",
                                      "location": "Lobby - North wall",
                                      "zoneId": 1
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<AccessPointResponseDTO>> create(
            @Valid @RequestBody AccessPointRequestDTO request
    ) {
        AccessPointResponseDTO created = accessPointService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Access point created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update access point")
    public ResponseEntity<ApiResponse<AccessPointResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody AccessPointRequestDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Access point updated successfully",
                accessPointService.update(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Delete access point",
            description = "Allowed only when no access events reference this access point"
    )
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        accessPointService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Access point deleted successfully"));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Activate access point", description = "Sets status to ACTIVE")
    public ResponseEntity<ApiResponse<AccessPointResponseDTO>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Access point activated successfully",
                accessPointService.activate(id)
        ));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Deactivate access point", description = "Sets status to INACTIVE")
    public ResponseEntity<ApiResponse<AccessPointResponseDTO>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Access point deactivated successfully",
                accessPointService.deactivate(id)
        ));
    }

    @PatchMapping("/{id}/maintenance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Put access point in maintenance", description = "Sets status to MAINTENANCE")
    public ResponseEntity<ApiResponse<AccessPointResponseDTO>> maintenance(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Access point set to maintenance",
                accessPointService.putInMaintenance(id)
        ));
    }
}
