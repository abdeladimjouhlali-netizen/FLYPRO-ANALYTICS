package com.flypro.analytics.accessprofile.dto;

import com.flypro.analytics.common.enums.SecurityLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.Set;

/**
 * Create / update payload for an access profile.
 */
@Schema(description = "Access profile create/update request")
public record AccessProfileRequestDTO(

        @NotBlank(message = "Profile name is required")
        @Size(min = 2, max = 150, message = "Profile name must be between 2 and 150 characters")
        @Schema(example = "Standard Office Access")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        @Schema(example = "Weekday office hours access to standard floors")
        String description,

        @NotNull(message = "Security level is required")
        @Schema(example = "MEDIUM")
        SecurityLevel securityLevel,

        @NotNull(message = "Start time is required")
        @Schema(example = "08:00:00")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        @Schema(example = "18:00:00")
        LocalTime endTime,

        @NotEmpty(message = "At least one zone must be assigned")
        @Schema(example = "[1, 2]", description = "Ids of authorized zones")
        Set<Long> zoneIds
) {
}
