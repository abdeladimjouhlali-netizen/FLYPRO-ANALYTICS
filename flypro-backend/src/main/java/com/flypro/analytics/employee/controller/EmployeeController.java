package com.flypro.analytics.employee.controller;

import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.common.response.ApiResponse;
import com.flypro.analytics.employee.dto.EmployeeRequestDTO;
import com.flypro.analytics.employee.dto.EmployeeResponseDTO;
import com.flypro.analytics.employee.dto.EmployeeSearchCriteria;
import com.flypro.analytics.employee.service.EmployeeService;
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
 * REST API for employee management.
 */
@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employees", description = "Employee lifecycle, search and status transitions")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Search employees", description = "Paginated search with filters and keyword")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponseDTO>>> search(
            @RequestParam(required = false) String employeeNumber,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String email,
            @Parameter(description = "Global keyword across number, names, email and position")
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(
                employeeNumber, firstName, lastName, departmentId, status, position, email, keyword
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Employees retrieved successfully",
                employeeService.search(criteria, pageable)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Get employee by id")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create employee")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = EmployeeRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Create employee",
                            value = """
                                    {
                                      "employeeNumber": "EMP-001",
                                      "firstName": "Sara",
                                      "lastName": "Benali",
                                      "email": "sara.benali@flypro.local",
                                      "phone": "+212612345678",
                                      "position": "Access Control Analyst",
                                      "status": "ACTIVE",
                                      "departmentId": 1,
                                      "accessProfileId": null
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> create(
            @Valid @RequestBody EmployeeRequestDTO request
    ) {
        EmployeeResponseDTO created = employeeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Employee created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update employee")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequestDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Employee updated successfully",
                employeeService.update(id, request)
        ));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Deactivate employee", description = "Sets status to INACTIVE")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Employee deactivated successfully",
                employeeService.deactivate(id)
        ));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Activate employee", description = "Sets status to ACTIVE")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Employee activated successfully",
                employeeService.activate(id)
        ));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Archive employee", description = "Sets status to ARCHIVED")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> archive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Employee archived successfully",
                employeeService.archive(id)
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Delete employee",
            description = "Permanent deletion is allowed only for ARCHIVED employees without active badge assignments"
    )
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Employee deleted successfully"));
    }
}
