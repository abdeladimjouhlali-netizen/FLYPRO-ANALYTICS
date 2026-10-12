package com.flypro.analytics.badge.controller;

import com.flypro.analytics.badge.dto.BadgeAssignmentDTO;
import com.flypro.analytics.badge.dto.BadgeRenewRequestDTO;
import com.flypro.analytics.badge.dto.BadgeReplaceRequestDTO;
import com.flypro.analytics.badge.dto.BadgeRequestDTO;
import com.flypro.analytics.badge.dto.BadgeResponseDTO;
import com.flypro.analytics.badge.dto.BadgeSearchCriteria;
import com.flypro.analytics.badge.service.BadgeService;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.BadgeType;
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
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/**
 * REST API for badge lifecycle and assignment management.
 */
@RestController
@RequestMapping("/api/v1/badges")
@Tag(name = "Badges", description = "Badge lifecycle, assignment, renew, replace and status transitions")
@SecurityRequirement(name = "bearerAuth")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Search badges", description = "Paginated search with filters and keyword")
    public ResponseEntity<ApiResponse<PageResponse<BadgeResponseDTO>>> search(
            @RequestParam(required = false) String badgeNumber,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) BadgeStatus status,
            @RequestParam(required = false) BadgeType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expirationDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expirationDateTo,
            @RequestParam(required = false) Long departmentId,
            @Parameter(description = "Global keyword across badge number, type, status and employee fields")
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "badgeNumber", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        BadgeSearchCriteria criteria = new BadgeSearchCriteria(
                badgeNumber, employeeId, status, type,
                expirationDateFrom, expirationDateTo, departmentId, keyword
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Badges retrieved successfully",
                badgeService.search(criteria, pageable)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Get badge by id")
    public ResponseEntity<ApiResponse<BadgeResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(badgeService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create badge")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = BadgeRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Create badge",
                            value = """
                                    {
                                      "badgeNumber": "BDG-001",
                                      "type": "RFID",
                                      "status": "PENDING_ACTIVATION",
                                      "issueDate": "2026-01-15",
                                      "expirationDate": "2027-01-15"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<BadgeResponseDTO>> create(
            @Valid @RequestBody BadgeRequestDTO request
    ) {
        BadgeResponseDTO created = badgeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Badge created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update badge")
    public ResponseEntity<ApiResponse<BadgeResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody BadgeRequestDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Badge updated successfully",
                badgeService.update(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Delete badge",
            description = "Allowed only when the badge has never been assigned"
    )
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        badgeService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Badge deleted successfully"));
    }

    @PostMapping("/{id}/assign/{employeeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Assign badge to employee")
    public ResponseEntity<ApiResponse<BadgeResponseDTO>> assign(
            @PathVariable Long id,
            @PathVariable Long employeeId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Badge assigned successfully",
                badgeService.assign(id, employeeId)
        ));
    }

    @PatchMapping("/{id}/unassign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Unassign badge", description = "Closes the active assignment while keeping history")
    public ResponseEntity<ApiResponse<BadgeResponseDTO>> unassign(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Badge unassigned successfully",
                badgeService.unassign(id)
        ));
    }

    @PatchMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Renew badge")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = BadgeRenewRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Renew badge",
                            value = """
                                    {
                                      "expirationDate": "2028-01-15",
                                      "issueDate": "2026-07-24"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<BadgeResponseDTO>> renew(
            @PathVariable Long id,
            @Valid @RequestBody BadgeRenewRequestDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Badge renewed successfully",
                badgeService.renew(id, request)
        ));
    }

    @PatchMapping("/{id}/replace")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Replace badge", description = "Transfers the active assignment to another badge")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = BadgeReplaceRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Replace badge",
                            value = """
                                    {
                                      "newBadgeId": 2
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<BadgeResponseDTO>> replace(
            @PathVariable Long id,
            @Valid @RequestBody BadgeReplaceRequestDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Badge replaced successfully",
                badgeService.replace(id, request)
        ));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Suspend badge")
    public ResponseEntity<ApiResponse<BadgeResponseDTO>> suspend(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Badge suspended successfully",
                badgeService.suspend(id)
        ));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Activate badge")
    public ResponseEntity<ApiResponse<BadgeResponseDTO>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Badge activated successfully",
                badgeService.activate(id)
        ));
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Revoke badge")
    public ResponseEntity<ApiResponse<BadgeResponseDTO>> revoke(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Badge revoked successfully",
                badgeService.revoke(id)
        ));
    }

    @PatchMapping("/{id}/lost")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Report badge as lost")
    public ResponseEntity<ApiResponse<BadgeResponseDTO>> reportLost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Badge reported as lost",
                badgeService.reportLost(id)
        ));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Get badge assignment history")
    public ResponseEntity<ApiResponse<List<BadgeAssignmentDTO>>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Badge history retrieved successfully",
                badgeService.getHistory(id)
        ));
    }
}
