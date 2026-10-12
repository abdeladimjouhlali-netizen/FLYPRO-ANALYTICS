package com.flypro.analytics.accesspoint.dto;

import com.flypro.analytics.common.enums.AccessPointStatus;
import com.flypro.analytics.common.enums.AccessPointType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create / update payload for an access point.
 */
@Schema(description = "Access point create/update request")
public record AccessPointRequestDTO(

        @NotBlank(message = "Access point name is required")
        @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
        @Schema(example = "Main Entrance Door")
        String name,

        @NotBlank(message = "Access point code is required")
        @Size(min = 2, max = 50, message = "Code must be between 2 and 50 characters")
        @Schema(example = "AP-MAIN-01")
        String code,

        @NotNull(message = "Type is required")
        @Schema(example = "DOOR")
        AccessPointType type,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        @Schema(example = "Primary entrance door reader")
        String description,

        @NotNull(message = "Status is required")
        @Schema(example = "ACTIVE")
        AccessPointStatus status,

        @Size(max = 45, message = "IP address must not exceed 45 characters")
        @Schema(example = "192.168.1.10")
        String ipAddress,

        @Size(max = 255, message = "Location must not exceed 255 characters")
        @Schema(example = "Lobby - North wall")
        String location,

        @NotNull(message = "Zone id is required")
        @Schema(example = "1")
        Long zoneId
) {
}
