package com.flypro.analytics.auth.controller;

import com.flypro.analytics.auth.dto.LoginRequest;
import com.flypro.analytics.auth.dto.LoginResponse;
import com.flypro.analytics.auth.dto.RefreshRequest;
import com.flypro.analytics.auth.dto.RefreshResponse;
import com.flypro.analytics.auth.service.AuthService;
import com.flypro.analytics.common.response.ApiResponse;
import com.flypro.analytics.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints d'authentification JWT.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, refresh, logout and current user")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT tokens")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue a new access token")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", response));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Revoke refresh token(s) and clear security context",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest
    ) {
        String accessToken = extractBearerToken(httpRequest);
        authService.logout(request != null ? request : new RefreshRequest(""), accessToken);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.message("Logout successful"));
    }

    private static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length()).trim();
        }
        return null;
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get the currently authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        return ResponseEntity.ok(ApiResponse.ok(authService.me()));
    }
}
