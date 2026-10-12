package com.flypro.analytics.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Optional API key gate for selected path prefixes (e.g. {@code /actuator/prometheus}).
 * Disabled unless {@code flypro.security.api-key.enabled=true}.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final boolean enabled;
    private final Set<String> keys;
    private final List<String> protectedPrefixes;

    public ApiKeyAuthFilter(
            @Value("${flypro.security.api-key.enabled:false}") boolean enabled,
            @Value("${flypro.security.api-key.keys:}") String keysCsv,
            @Value("${flypro.security.api-key.protected-prefixes:/actuator/prometheus}") String prefixesCsv
    ) {
        this.enabled = enabled;
        this.keys = splitCsv(keysCsv).collect(Collectors.toSet());
        this.protectedPrefixes = splitCsv(prefixesCsv).toList();
    }

    private static java.util.stream.Stream<String> splitCsv(String csv) {
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        return protectedPrefixes.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String provided = request.getHeader(API_KEY_HEADER);
        if (provided == null || !keys.contains(provided)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"timestamp\":\"" + Instant.now()
                            + "\",\"status\":403,\"error\":\"Forbidden\","
                            + "\"code\":\"API_KEY_REQUIRED\",\"message\":\"A valid X-API-Key header is required for this endpoint.\","
                            + "\"path\":\"" + request.getRequestURI() + "\"}"
            );
            return;
        }
        filterChain.doFilter(request, response);
    }
}
