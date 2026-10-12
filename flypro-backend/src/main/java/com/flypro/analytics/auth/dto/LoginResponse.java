package com.flypro.analytics.auth.dto;

import com.flypro.analytics.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Réponse de connexion (access + refresh tokens).
 */
@Schema(description = "Jetons émis après authentification")
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
