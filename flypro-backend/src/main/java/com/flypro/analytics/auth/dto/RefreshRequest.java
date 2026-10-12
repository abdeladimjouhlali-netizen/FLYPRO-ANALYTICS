package com.flypro.analytics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Requête de renouvellement de jeton.
 */
@Schema(description = "Refresh token")
public record RefreshRequest(

        @NotBlank(message = "Refresh token is required")
        @Schema(description = "Refresh JWT")
        String refreshToken
) {
}
