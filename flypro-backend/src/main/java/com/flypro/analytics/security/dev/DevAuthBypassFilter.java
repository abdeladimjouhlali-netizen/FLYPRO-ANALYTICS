package com.flypro.analytics.security.dev;

import com.flypro.analytics.security.userdetails.CustomUserDetailsService;
import com.flypro.analytics.security.userdetails.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * DEV ONLY: injects a fixed authenticated principal so JWT can be ignored temporarily.
 * Enable with {@code flypro.security.auth-enabled=false}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@ConditionalOnProperty(prefix = "flypro.security", name = "auth-enabled", havingValue = "false")
public class DevAuthBypassFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DevAuthBypassFilter.class);

    private final CustomUserDetailsService userDetailsService;
    private final String bypassUsername;

    public DevAuthBypassFilter(
            CustomUserDetailsService userDetailsService,
            @Value("${flypro.security.dev-bypass-username:admin}") String bypassUsername
    ) {
        this.userDetailsService = userDetailsService;
        this.bypassUsername = bypassUsername;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(bypassUsername);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ex) {
                log.warn("Dev auth bypass failed for user={}: {}", bypassUsername, ex.toString());
            }
        }
        filterChain.doFilter(request, response);
    }
}
