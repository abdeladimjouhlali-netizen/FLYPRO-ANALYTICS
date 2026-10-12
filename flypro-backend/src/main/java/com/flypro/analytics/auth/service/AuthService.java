package com.flypro.analytics.auth.service;

import com.flypro.analytics.auth.dto.LoginRequest;
import com.flypro.analytics.auth.dto.LoginResponse;
import com.flypro.analytics.auth.dto.RefreshRequest;
import com.flypro.analytics.auth.dto.RefreshResponse;
import com.flypro.analytics.auth.entity.RefreshToken;
import com.flypro.analytics.auth.repository.RefreshTokenRepository;
import com.flypro.analytics.common.exceptions.BadRequestException;
import com.flypro.analytics.infra.redis.OptionalJwtBlacklist;
import com.flypro.analytics.security.bruteforce.LoginAttemptService;
import com.flypro.analytics.security.jwt.JwtService;
import com.flypro.analytics.security.jwt.TokenType;
import com.flypro.analytics.security.userdetails.UserPrincipal;
import com.flypro.analytics.user.dto.UserResponse;
import com.flypro.analytics.user.entity.User;
import com.flypro.analytics.user.mapper.UserMapper;
import com.flypro.analytics.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Cas d'utilisation d'authentification (login, refresh, logout, me).
 */
@Service
public class AuthService {

    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final LoginAttemptService loginAttemptService;
    private final OptionalJwtBlacklist jwtBlacklist;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       UserMapper userMapper,
                       LoginAttemptService loginAttemptService,
                       OptionalJwtBlacklist jwtBlacklist) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userMapper = userMapper;
        this.loginAttemptService = loginAttemptService;
        this.jwtBlacklist = jwtBlacklist;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        loginAttemptService.assertNotLocked(request.username());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findByUsername(principal.getUsername())
                    .orElseThrow(() -> new BadRequestException("Authenticated user not found"));

            refreshTokenRepository.revokeAllByUser(user);

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            persistRefreshToken(user, refreshToken);
            loginAttemptService.loginSucceeded(request.username());

            return new LoginResponse(
                    accessToken,
                    refreshToken,
                    TOKEN_TYPE_BEARER,
                    jwtService.getAccessTokenExpirationMs() / 1000,
                    userMapper.toResponse(user)
            );
        } catch (BadCredentialsException ex) {
            loginAttemptService.loginFailed(request.username());
            throw ex;
        }
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        String rawToken = request.refreshToken();

        if (!jwtService.isTokenValid(rawToken, TokenType.REFRESH)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        RefreshToken stored = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new BadRequestException("Refresh token not recognized"));

        if (stored.isRevoked() || stored.isExpired()) {
            throw new BadRequestException("Refresh token has been revoked or expired");
        }

        User user = stored.getUser();
        if (!user.isEnabled() || user.isAccountLocked()) {
            throw new BadRequestException("User account is disabled or locked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        persistRefreshToken(user, newRefreshToken);

        return new RefreshResponse(
                newAccessToken,
                newRefreshToken,
                TOKEN_TYPE_BEARER,
                jwtService.getAccessTokenExpirationMs() / 1000
        );
    }

    @Transactional
    public void logout(RefreshRequest request, String accessToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            jwtBlacklist.blacklist(accessToken);
        }
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            refreshTokenRepository.findByToken(request.refreshToken())
                    .ifPresent(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                    });
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal principal) {
            userRepository.findByUsername(principal.getUsername())
                    .ifPresent(refreshTokenRepository::revokeAllByUser);
        }

        SecurityContextHolder.clearContext();
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "No authenticated user");
        }
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BadRequestException("Authenticated user not found"));
        return userMapper.toResponse(user);
    }

    private void persistRefreshToken(User user, String token) {
        RefreshToken entity = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);
    }
}
