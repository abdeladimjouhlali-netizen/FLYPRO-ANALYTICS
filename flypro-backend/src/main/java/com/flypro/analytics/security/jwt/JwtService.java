package com.flypro.analytics.security.jwt;

import com.flypro.analytics.common.enums.RoleName;
import com.flypro.analytics.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Génération et validation des JWT (access + refresh).
 */
@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = buildKey(properties.getJwtSecret());
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.getAccessTokenExpirationMs());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.getIssuer())
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("role", user.getRole().getName().name())
                .claim("type", TokenType.ACCESS.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.getRefreshTokenExpirationMs());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.getIssuer())
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("type", TokenType.REFRESH.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public boolean isTokenValid(String token, TokenType expectedType) {
        try {
            Claims claims = parseClaims(token);
            String type = claims.get("type", String.class);
            return expectedType.name().equals(type) && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return true;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public RoleName extractRole(String token) {
        return RoleName.valueOf(parseClaims(token).get("role", String.class));
    }

    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    public Instant extractExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpirationMs() {
        return properties.getAccessTokenExpirationMs();
    }

    public long getRefreshTokenExpirationMs() {
        return properties.getRefreshTokenExpirationMs();
    }

    private SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured (flypro.security.jwt-secret / JWT_SECRET)");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes)");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
