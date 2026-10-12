package com.flypro.analytics.user.dto;

import com.flypro.analytics.common.enums.RoleName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Représentation publique d'un utilisateur (sans mot de passe).
 */
@Schema(description = "Utilisateur authentifié")
public record UserResponse(
        Long id,
        String username,
        String email,
        boolean enabled,
        boolean accountLocked,
        RoleName role,
        Long employeeId,
        Instant createdAt,
        Instant updatedAt
) {
}
