package com.flypro.analytics.infra.security;

import com.flypro.analytics.infra.redis.OptionalJwtBlacklist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtBlacklistFilterTest {

    @Mock
    private OptionalJwtBlacklist blacklist;

    @Test
    @DisplayName("rejects blacklisted bearer token with 401")
    void rejectsBlacklisted() throws Exception {
        when(blacklist.isBlacklisted("revoked")).thenReturn(true);
        JwtBlacklistFilter filter = new JwtBlacklistFilter(blacklist);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer revoked");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("TOKEN_REVOKED");
    }

    @Test
    @DisplayName("blacklists token after successful logout")
    void blacklistsOnLogout() throws Exception {
        when(blacklist.isBlacklisted("ok-token")).thenReturn(false);
        JwtBlacklistFilter filter = new JwtBlacklistFilter(blacklist);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
        request.setRequestURI("/api/v1/auth/logout");
        request.addHeader("Authorization", "Bearer ok-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, new MockFilterChain());

        verify(blacklist).blacklist("ok-token");
    }
}
