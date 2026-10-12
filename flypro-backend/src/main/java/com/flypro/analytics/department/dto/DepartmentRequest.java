package com.flypro.analytics.department.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create / update payload for a department.
 */
@Schema(description = "Department create/update request")
public record DepartmentRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
        @Schema(example = "Human Resources", description = "Unique department name")
        String name,

        @NotBlank(message = "Code is required")
        @Size(min = 2, max = 50, message = "Code must be between 2 and 50 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]+$",
                message = "Code must contain only letters, digits, underscore or hyphen"
        )
        @Schema(example = "HR", description = "Unique department code (stored uppercase)")
        String code,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        @Schema(example = "Human resources and payroll")
        String description
) {
}
