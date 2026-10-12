package com.flypro.analytics.accessevent.controller;

import com.flypro.analytics.accessevent.dto.AccessEventRequestDTO;
import com.flypro.analytics.accessevent.dto.AccessEventResponseDTO;
import com.flypro.analytics.accessevent.dto.AccessEventSearchCriteria;
import com.flypro.analytics.accessevent.service.AccessEventService;
import com.flypro.analytics.common.enums.AccessDirection;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.EventSource;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * REST API for access event recording and search.
 * Events are append-only (no update / delete endpoints).
 */
@RestController
@RequestMapping("/api/v1/access-events")
@Tag(name = "Access Events", description = "Record and search badge access attempts")
@SecurityRequirement(name = "bearerAuth")
public class AccessEventController {

    private final AccessEventService accessEventService;

    public AccessEventController(AccessEventService accessEventService) {
        this.accessEventService = accessEventService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Search access events", description = "Paginated search with filters and date range")
    public ResponseEntity<ApiResponse<PageResponse<AccessEventResponseDTO>>> search(
            @RequestParam(required = false) String badgeNumber,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long accessPointId,
            @RequestParam(required = false) AccessResult result,
            @RequestParam(required = false) AccessDirection direction,
            @RequestParam(required = false) EventSource source,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @Parameter(description = "Global keyword across badge, employee, access point and zone")
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "eventTimestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        AccessEventSearchCriteria criteria = new AccessEventSearchCriteria(
                badgeNumber, employeeId, departmentId, zoneId, accessPointId,
                result, direction, source, from, to, keyword
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Access events retrieved successfully",
                accessEventService.search(criteria, pageable)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Get access event by id")
    public ResponseEntity<ApiResponse<AccessEventResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(accessEventService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create access event")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = AccessEventRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Authorized entry",
                            value = """
                                    {
                                      "eventTimestamp": "2026-07-26T10:15:30Z",
                                      "direction": "ENTRY",
                                      "result": "AUTHORIZED",
                                      "refusalReason": null,
                                      "source": "API",
                                      "rawPayload": "{\\"readerId\\":\\"R-01\\"}",
                                      "badgeId": 1,
                                      "accessPointId": 1,
                                      "zoneId": 1
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<AccessEventResponseDTO>> create(
            @Valid @RequestBody AccessEventRequestDTO request
    ) {
        AccessEventResponseDTO created = accessEventService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Access event created successfully", created));
    }
}
