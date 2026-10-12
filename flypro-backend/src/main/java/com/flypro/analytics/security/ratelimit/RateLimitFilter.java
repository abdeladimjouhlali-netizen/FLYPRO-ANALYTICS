package com.flypro.analytics.security.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window rate limiter for sensitive endpoints (login/refresh).
 * Infrastructure-only: does not alter API contracts.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMillis;

    public RateLimitFilter(
            @Value("${flypro.security.rate-limit.max-requests:30}") int maxRequests,
            @Value("${flypro.security.rate-limit.window-seconds:60}") long windowSeconds
    ) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.endsWith("/api/v1/auth/login") || path.endsWith("/api/v1/auth/refresh"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String key = clientKey(request) + "|" + request.getRequestURI();
        long now = Instant.now().toEpochMilli();
        Deque<Long> hits = windows.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (hits) {
            while (!hits.isEmpty() && now - hits.peekFirst() > windowMillis) {
                hits.pollFirst();
            }
            if (hits.size() >= maxRequests) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"timestamp\":\"" + Instant.now()
                                + "\",\"status\":429,\"error\":\"Too Many Requests\","
                                + "\"code\":\"RATE_LIMIT_EXCEEDED\","
                                + "\"message\":\"Too many requests. Please try again later.\","
                                + "\"path\":\"" + request.getRequestURI() + "\"}"
                );
                return;
            }
            hits.addLast(now);
        }
        filterChain.doFilter(request, response);
    }

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
