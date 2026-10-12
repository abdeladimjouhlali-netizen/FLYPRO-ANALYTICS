package com.flypro.analytics.zone.controller;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.common.response.ApiResponse;
import com.flypro.analytics.zone.dto.ZoneRequestDTO;
import com.flypro.analytics.zone.dto.ZoneResponseDTO;
import com.flypro.analytics.zone.dto.ZoneSearchCriteria;
import com.flypro.analytics.zone.service.ZoneService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for zone management.
 */
@RestController
@RequestMapping("/api/v1/zones")
@Tag(name = "Zones", description = "Zone CRUD, search and sensitive-area identification")
@SecurityRequirement(name = "bearerAuth")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Search zones", description = "Paginated search with filters and keyword")
    public ResponseEntity<ApiResponse<PageResponse<ZoneResponseDTO>>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String floor,
            @RequestParam(required = false) Boolean sensitive,
            @Parameter(description = "Global keyword across name, building, floor and description")
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        ZoneSearchCriteria criteria = new ZoneSearchCriteria(name, building, floor, sensitive, keyword);
        return ResponseEntity.ok(ApiResponse.ok(
                "Zones retrieved successfully",
                zoneService.search(criteria, pageable)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Get zone by id")
    public ResponseEntity<ApiResponse<ZoneResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(zoneService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create zone")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = ZoneRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Create sensitive zone",
                            value = """
                                    {
                                      "name": "Server Room A",
                                      "building": "Building North",
                                      "floor": "3",
                                      "sensitive": true,
                                      "description": "Restricted server room"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<ZoneResponseDTO>> create(
            @Valid @RequestBody ZoneRequestDTO request
    ) {
        ZoneResponseDTO created = zoneService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Zone created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update zone")
    public ResponseEntity<ApiResponse<ZoneResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody ZoneRequestDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Zone updated successfully",
                zoneService.update(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Delete zone",
            description = "Allowed only when the zone has no access points and is not assigned to any access profile"
    )
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        zoneService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Zone deleted successfully"));
    }
}
