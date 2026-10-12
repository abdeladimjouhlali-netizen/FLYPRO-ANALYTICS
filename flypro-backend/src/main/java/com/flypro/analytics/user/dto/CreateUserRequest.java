package com.flypro.analytics.user.dto;

import com.flypro.analytics.common.enums.RoleName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Création d'un compte console (Admin / Analyste) — réservé au SUPER_ADMIN.
 */
@Schema(description = "Création utilisateur par le Super Admin")
public record CreateUserRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Schema(example = "opsadmin2")
        String username,

        @NotBlank
        @Email
        @Size(max = 255)
        @Schema(example = "opsadmin2@flypro.local")
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        @Schema(example = "Admin@123")
        String password,

        @NotNull
        @Schema(example = "ROLE_ADMIN", description = "ROLE_ADMIN ou ROLE_ANALYST uniquement")
        RoleName role
) {
}
