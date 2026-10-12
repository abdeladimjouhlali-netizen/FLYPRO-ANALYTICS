package com.flypro.analytics.user.controller;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.common.response.ApiResponse;
import com.flypro.analytics.user.dto.CreateUserRequest;
import com.flypro.analytics.user.dto.UserResponse;
import com.flypro.analytics.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion des comptes console — réservée au Super Admin.
 * Seul le Super Admin peut créer un Administrateur (ROLE_ADMIN).
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Gestion des utilisateurs — Super Admin uniquement")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Lister les utilisateurs")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> list(
            @PageableDefault(size = 50, sort = "username", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.list(pageable)));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Créer un Admin ou Analyste (Super Admin uniquement)")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.createBySuperAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Utilisateur créé avec succès", created));
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Activer / désactiver un utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.setEnabled(id, enabled)));
    }
}
