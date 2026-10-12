package com.flypro.analytics.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés JWT chargées depuis application.yml (préfixe flypro.security).
 */
@ConfigurationProperties(prefix = "flypro.security")
public class JwtProperties {

    /**
     * Secret HMAC (Base64 ou texte brut long) — fourni via JWT_SECRET.
     */
    private String jwtSecret;

    /**
     * Durée de vie du access token (ms). Défaut : 15 minutes.
     */
    private long accessTokenExpirationMs = 900_000L;

    /**
     * Durée de vie du refresh token (ms). Défaut : 7 jours.
     */
    private long refreshTokenExpirationMs = 604_800_000L;

    /**
     * Émetteur JWT.
     */
    private String issuer = "flypro-analytics";

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public void setAccessTokenExpirationMs(long accessTokenExpirationMs) {
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public void setRefreshTokenExpirationMs(long refreshTokenExpirationMs) {
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
