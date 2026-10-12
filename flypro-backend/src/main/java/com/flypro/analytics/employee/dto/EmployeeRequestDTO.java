package com.flypro.analytics.employee.dto;

import com.flypro.analytics.common.enums.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create / update payload for an employee.
 */
@Schema(description = "Employee create/update request")
public record EmployeeRequestDTO(

        @NotBlank(message = "Employee number is required")
        @Size(min = 2, max = 50, message = "Employee number must be between 2 and 50 characters")
        @Schema(example = "EMP-001")
        String employeeNumber,

        @NotBlank(message = "First name is required")
        @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
        @Schema(example = "Sara")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
        @Schema(example = "Benali")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255)
        @Schema(example = "sara.benali@flypro.local")
        String email,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        @Pattern(regexp = "^$|^[+0-9()\\-\\s]+$", message = "Invalid phone number format")
        @Schema(example = "+212612345678")
        String phone,

        @NotBlank(message = "Position is required")
        @Size(min = 2, max = 150, message = "Position must be between 2 and 150 characters")
        @Schema(example = "Access Control Analyst")
        String position,

        @NotNull(message = "Status is required")
        @Schema(example = "ACTIVE")
        EmployeeStatus status,

        @NotNull(message = "Department id is required")
        @Schema(example = "1")
        Long departmentId,

        @Schema(example = "null", description = "Optional access profile id (nullable for now)")
        Long accessProfileId
) {
}
