package com.flypro.analytics.zone.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create / update payload for a zone.
 */
@Schema(description = "Zone create/update request")
public record ZoneRequestDTO(

        @NotBlank(message = "Zone name is required")
        @Size(min = 2, max = 150, message = "Zone name must be between 2 and 150 characters")
        @Schema(example = "Server Room A")
        String name,

        @NotBlank(message = "Building is required")
        @Size(min = 1, max = 150, message = "Building must be between 1 and 150 characters")
        @Schema(example = "Building North")
        String building,

        @NotBlank(message = "Floor is required")
        @Size(min = 1, max = 50, message = "Floor must be between 1 and 50 characters")
        @Schema(example = "3")
        String floor,

        @NotNull(message = "Sensitive flag is required")
        @Schema(example = "true", description = "Marks the zone as sensitive")
        Boolean sensitive,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        @Schema(example = "Restricted server room with biometric access")
        String description
) {
}
