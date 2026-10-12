package com.flypro.analytics.infra.security;

import com.flypro.analytics.infra.redis.OptionalJwtBlacklist;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Rejects requests bearing a revoked (logged-out) access token.
 * No-op when Redis is disabled ({@link OptionalJwtBlacklist} then always reports "not blacklisted").
 */
@Component
public class JwtBlacklistFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final OptionalJwtBlacklist blacklist;

    public JwtBlacklistFilter(OptionalJwtBlacklist blacklist) {
        this.blacklist = blacklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (blacklist.isBlacklisted(token)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"timestamp\":\"" + Instant.now()
                                + "\",\"status\":401,\"error\":\"Unauthorized\","
                                + "\"code\":\"TOKEN_REVOKED\",\"message\":\"This token has been revoked.\","
                                + "\"path\":\"" + request.getRequestURI() + "\"}"
                );
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
