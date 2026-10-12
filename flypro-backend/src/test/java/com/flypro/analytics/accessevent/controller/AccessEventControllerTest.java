package com.flypro.analytics.accessevent.controller;

import com.flypro.analytics.accessevent.dto.AccessEventResponseDTO;
import com.flypro.analytics.accessevent.exception.AccessEventNotFoundException;
import com.flypro.analytics.accessevent.exception.InvalidAccessEventException;
import com.flypro.analytics.accessevent.service.AccessEventService;
import com.flypro.analytics.accesspoint.exception.AccessPointNotFoundException;
import com.flypro.analytics.badge.exception.BadgeNotFoundException;
import com.flypro.analytics.common.enums.AccessDirection;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.EventSource;
import com.flypro.analytics.common.exceptions.GlobalExceptionHandler;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.security.handler.SecurityExceptionHandler;
import com.flypro.analytics.security.jwt.JwtAuthenticationFilter;
import com.flypro.analytics.infra.security.ApiKeyAuthFilter;
import com.flypro.analytics.infra.security.JwtBlacklistFilter;
import com.flypro.analytics.security.jwt.JwtService;
import com.flypro.analytics.security.userdetails.CustomUserDetailsService;
import com.flypro.analytics.zone.exception.ZoneNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccessEventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AccessEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessEventService accessEventService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private JwtBlacklistFilter jwtBlacklistFilter;
    @MockitoBean
    private ApiKeyAuthFilter apiKeyAuthFilter;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private SecurityExceptionHandler securityExceptionHandler;

    private final AccessEventResponseDTO sample = new AccessEventResponseDTO(
            100L, Instant.parse("2026-07-26T10:15:30Z"), AccessDirection.ENTRY, AccessResult.AUTHORIZED,
            null, EventSource.API, "{}", 3L, "BDG-001", BadgeStatus.ACTIVE,
            4L, "EMP-1", "Sara", "Benali", 2L, "AP-01", "Main Door", null, null
    );

    private final String validBody = """
            {
              "eventTimestamp": "2026-07-26T10:15:30Z",
              "direction": "ENTRY",
              "result": "AUTHORIZED",
              "source": "API",
              "rawPayload": "{}",
              "badgeId": 3,
              "accessPointId": 2,
              "zoneId": 1
            }
            """;

    @Test
    @DisplayName("POST create access event")
    void create() throws Exception {
        when(accessEventService.create(any())).thenReturn(sample);

        mockMvc.perform(post("/api/v1/access-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.result").value("AUTHORIZED"));
    }

    @Test
    @DisplayName("POST validation error")
    void createValidation() throws Exception {
        mockMvc.perform(post("/api/v1/access-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventTimestamp":null,"direction":null,"result":null,"source":null,"badgeId":null,"accessPointId":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST not found / invalid event")
    void createErrors() throws Exception {
        doThrow(new BadgeNotFoundException(99L)).when(accessEventService).create(any());
        mockMvc.perform(post("/api/v1/access-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BADGE_NOT_FOUND"));

        doThrow(new AccessPointNotFoundException(99L)).when(accessEventService).create(any());
        mockMvc.perform(post("/api/v1/access-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCESS_POINT_NOT_FOUND"));

        doThrow(new ZoneNotFoundException(99L)).when(accessEventService).create(any());
        mockMvc.perform(post("/api/v1/access-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ZONE_NOT_FOUND"));

        doThrow(new InvalidAccessEventException("Zone mismatch")).when(accessEventService).create(any());
        mockMvc.perform(post("/api/v1/access-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_EVENT"));
    }

    @Test
    @DisplayName("GET by id and search")
    void read() throws Exception {
        when(accessEventService.getById(100L)).thenReturn(sample);
        when(accessEventService.search(any(), any())).thenReturn(
                new PageResponse<>(List.of(sample), 0, 20, 1, 1, true, true)
        );

        mockMvc.perform(get("/api/v1/access-events/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.badgeNumber").value("BDG-001"));

        mockMvc.perform(get("/api/v1/access-events")
                        .param("result", "AUTHORIZED")
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-07-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET not found")
    void notFound() throws Exception {
        when(accessEventService.getById(99L)).thenThrow(new AccessEventNotFoundException(99L));

        mockMvc.perform(get("/api/v1/access-events/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCESS_EVENT_NOT_FOUND"));
    }
}
