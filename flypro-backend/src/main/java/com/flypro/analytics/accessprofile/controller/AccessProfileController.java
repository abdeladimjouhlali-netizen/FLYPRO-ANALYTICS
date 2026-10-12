package com.flypro.analytics.accessprofile.controller;

import com.flypro.analytics.accessprofile.dto.AccessProfileRequestDTO;
import com.flypro.analytics.accessprofile.dto.AccessProfileResponseDTO;
import com.flypro.analytics.accessprofile.dto.AccessProfileSearchCriteria;
import com.flypro.analytics.accessprofile.service.AccessProfileService;
import com.flypro.analytics.common.enums.SecurityLevel;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for access profile management and zone assignment.
 */
@RestController
@RequestMapping("/api/v1/access-profiles")
@Tag(name = "Access Profiles", description = "Access profile CRUD, search and zone assignment")
@SecurityRequirement(name = "bearerAuth")
public class AccessProfileController {

    private final AccessProfileService accessProfileService;

    public AccessProfileController(AccessProfileService accessProfileService) {
        this.accessProfileService = accessProfileService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Search access profiles")
    public ResponseEntity<ApiResponse<PageResponse<AccessProfileResponseDTO>>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) SecurityLevel securityLevel,
            @RequestParam(required = false) Long zoneId,
            @Parameter(description = "Global keyword across name and description")
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        AccessProfileSearchCriteria criteria = new AccessProfileSearchCriteria(
                name, securityLevel, zoneId, keyword
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Access profiles retrieved successfully",
                accessProfileService.search(criteria, pageable)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Get access profile by id")
    public ResponseEntity<ApiResponse<AccessProfileResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(accessProfileService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create access profile")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = AccessProfileRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Create access profile",
                            value = """
                                    {
                                      "name": "Standard Office Access",
                                      "description": "Weekday office hours",
                                      "securityLevel": "MEDIUM",
                                      "startTime": "08:00:00",
                                      "endTime": "18:00:00",
                                      "zoneIds": [1, 2]
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<AccessProfileResponseDTO>> create(
            @Valid @RequestBody AccessProfileRequestDTO request
    ) {
        AccessProfileResponseDTO created = accessProfileService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Access profile created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update access profile")
    public ResponseEntity<ApiResponse<AccessProfileResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody AccessProfileRequestDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Access profile updated successfully",
                accessProfileService.update(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Delete access profile",
            description = "Allowed only when no employees are assigned to the profile"
    )
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        accessProfileService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Access profile deleted successfully"));
    }

    @PostMapping("/{id}/zones/{zoneId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Assign zone to access profile")
    public ResponseEntity<ApiResponse<AccessProfileResponseDTO>> assignZone(
            @PathVariable Long id,
            @PathVariable Long zoneId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Zone assigned successfully",
                accessProfileService.assignZone(id, zoneId)
        ));
    }

    @DeleteMapping("/{id}/zones/{zoneId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Remove zone from access profile",
            description = "At least one zone must remain assigned"
    )
    public ResponseEntity<ApiResponse<AccessProfileResponseDTO>> removeZone(
            @PathVariable Long id,
            @PathVariable Long zoneId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Zone removed successfully",
                accessProfileService.removeZone(id, zoneId)
        ));
    }
}
