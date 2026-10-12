package com.flypro.analytics.alert.dto;

import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload to manually create an alert (also used internally by the rule engine mapping).
 */
@Schema(description = "Alert create request")
public record AlertRequestDTO(

        @NotBlank
        @Size(max = 200)
        @Schema(example = "Badge revoked during access attempt")
        String title,

        @Size(max = 2000)
        @Schema(example = "Badge BDG-001 is revoked")
        String description,

        @NotNull
        @Schema(example = "HIGH")
        AlertSeverity severity,

        @NotNull
        @Schema(example = "BADGE_REVOKED")
        AlertType type,

        @Schema(example = "1")
        Long employeeId,

        @Schema(example = "1")
        Long badgeId,

        @Schema(example = "10")
        Long accessEventId
) {
}
