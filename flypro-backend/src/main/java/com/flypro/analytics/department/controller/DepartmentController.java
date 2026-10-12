package com.flypro.analytics.department.controller;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.common.response.ApiResponse;
import com.flypro.analytics.department.dto.DepartmentRequest;
import com.flypro.analytics.department.dto.DepartmentResponse;
import com.flypro.analytics.department.dto.DepartmentSearchCriteria;
import com.flypro.analytics.department.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * REST API for department management.
 */
@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "Departments", description = "CRUD, search and soft-delete for organizational departments")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @Operation(
            summary = "Search departments",
            description = "Returns a paginated and sortable list of departments. "
                    + "Supports filtering by name, code, active flag and creation date range."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Departments retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    public ResponseEntity<ApiResponse<PageResponse<DepartmentResponse>>> search(
            @Parameter(description = "Filter by name (partial, case-insensitive)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filter by code (partial, case-insensitive)")
            @RequestParam(required = false) String code,
            @Parameter(description = "Filter by active flag")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Created at from (ISO-8601)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @Parameter(description = "Created at to (ISO-8601)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        DepartmentSearchCriteria criteria = new DepartmentSearchCriteria(
                name, code, active, createdFrom, createdTo
        );
        PageResponse<DepartmentResponse> page = departmentService.search(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Departments retrieved successfully", page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by id")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getById(
            @Parameter(description = "Department id", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.getById(id)));
    }

    @PostMapping
    @Operation(
            summary = "Create department",
            description = "Creates a new department. Code must be unique and uppercase."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = DepartmentRequest.class),
                    examples = @ExampleObject(
                            name = "Create HR",
                            value = """
                                    {
                                      "name": "Human Resources",
                                      "code": "HR",
                                      "description": "Human resources and payroll"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(
            @Valid @RequestBody DepartmentRequest request
    ) {
        DepartmentResponse created = departmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Department created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request
    ) {
        DepartmentResponse updated = departmentService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Department updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete or deactivate department",
            description = "Hard-deletes the department when it has no employees. "
                    + "If employees are assigned, the department is soft-deactivated (active=false)."
    )
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        DepartmentResponse result = departmentService.delete(id);
        if (result == null) {
            return ResponseEntity.ok(ApiResponse.message("Department deleted successfully"));
        }
        return ResponseEntity.ok(ApiResponse.ok(
                "Department deactivated because employees are still assigned",
                result
        ));
    }
}
