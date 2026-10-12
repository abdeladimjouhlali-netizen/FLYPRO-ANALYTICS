package com.flypro.analytics.security.jwt;

import com.flypro.analytics.common.enums.RoleName;
import com.flypro.analytics.role.entity.Role;
import com.flypro.analytics.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du service JWT (génération, validation, expiration).
 */
class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setJwtSecret("test-jwt-secret-key-must-be-at-least-32-bytes-long");
        properties.setAccessTokenExpirationMs(1_000L);
        properties.setRefreshTokenExpirationMs(5_000L);
        properties.setIssuer("flypro-analytics-test");
        jwtService = new JwtService(properties);

        Role role = Role.builder()
                .name(RoleName.ROLE_SUPER_ADMIN)
                .description("admin")
                .build();
        role.setId(1L);

        user = User.builder()
                .username("admin")
                .email("admin@flypro.local")
                .password("encoded")
                .enabled(true)
                .accountLocked(false)
                .role(role)
                .build();
        user.setId(1L);
    }

    @Test
    void generateAndValidateAccessToken() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token, TokenType.ACCESS)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.extractRole(token)).isEqualTo(RoleName.ROLE_SUPER_ADMIN);
        assertThat(jwtService.isTokenValid(token, TokenType.REFRESH)).isFalse();
    }

    @Test
    void generateAndValidateRefreshToken() {
        String token = jwtService.generateRefreshToken(user);

        assertThat(jwtService.isTokenValid(token, TokenType.REFRESH)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.isTokenValid(token, TokenType.ACCESS)).isFalse();
    }

    @Test
    void expiredAccessTokenIsDetected() throws InterruptedException {
        JwtProperties shortLived = new JwtProperties();
        shortLived.setJwtSecret("test-jwt-secret-key-must-be-at-least-32-bytes-long");
        shortLived.setAccessTokenExpirationMs(1L);
        shortLived.setRefreshTokenExpirationMs(5_000L);
        shortLived.setIssuer("flypro-analytics-test");
        JwtService shortJwt = new JwtService(shortLived);

        String token = shortJwt.generateAccessToken(user);
        Thread.sleep(20L);

        assertThat(shortJwt.isTokenExpired(token)).isTrue();
        assertThat(shortJwt.isTokenValid(token, TokenType.ACCESS)).isFalse();
    }

    @Test
    void invalidTokenIsRejected() {
        assertThat(jwtService.isTokenValid("not.a.jwt", TokenType.ACCESS)).isFalse();
    }
}
