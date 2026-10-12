package com.flypro.analytics.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Réponse de refresh (nouveaux jetons).
 */
@Schema(description = "Nouveaux jetons après refresh")
public record RefreshResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
