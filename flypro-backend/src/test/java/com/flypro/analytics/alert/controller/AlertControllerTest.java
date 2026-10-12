package com.flypro.analytics.alert.controller;

import com.flypro.analytics.alert.dto.AlertResponseDTO;
import com.flypro.analytics.alert.exception.AlertNotFoundException;
import com.flypro.analytics.alert.exception.InvalidAlertStateException;
import com.flypro.analytics.alert.service.AlertService;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.common.exceptions.GlobalExceptionHandler;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.security.handler.SecurityExceptionHandler;
import com.flypro.analytics.security.jwt.JwtAuthenticationFilter;
import com.flypro.analytics.infra.security.ApiKeyAuthFilter;
import com.flypro.analytics.infra.security.JwtBlacklistFilter;
import com.flypro.analytics.security.jwt.JwtService;
import com.flypro.analytics.security.userdetails.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AlertController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService alertService;
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

    private final AlertResponseDTO sample = new AlertResponseDTO(
            1L, "Access denied", "desc", AlertSeverity.MEDIUM, AlertStatus.OPEN,
            AlertType.ACCESS_DENIED, Instant.parse("2026-07-26T10:00:00Z"), null, null,
            4L, "EMP-1", "A B", 3L, "BDG-001", 10L, 1L, "Zone", 2L, null
    );

    @Test
    @DisplayName("GET alerts search")
    void search() throws Exception {
        when(alertService.search(any(), any())).thenReturn(PageResponse.from(new PageImpl<>(List.of(sample))));

        mockMvc.perform(get("/api/v1/alerts").param("status", "OPEN").param("severity", "MEDIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("PATCH resolve")
    void resolve() throws Exception {
        AlertResponseDTO resolved = new AlertResponseDTO(
                1L, "Access denied", "desc", AlertSeverity.MEDIUM, AlertStatus.RESOLVED,
                AlertType.ACCESS_DENIED, Instant.parse("2026-07-26T10:00:00Z"),
                Instant.parse("2026-07-26T11:00:00Z"), "admin",
                4L, "EMP-1", "A B", 3L, "BDG-001", 10L, 1L, "Zone", 2L, null
        );
        when(alertService.resolve(1L)).thenReturn(resolved);

        mockMvc.perform(patch("/api/v1/alerts/1/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("GET by id not found")
    void notFound() throws Exception {
        when(alertService.getById(99L)).thenThrow(new AlertNotFoundException(99L));

        mockMvc.perform(get("/api/v1/alerts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ALERT_NOT_FOUND"));
    }

    @Test
    @DisplayName("invalid state returns 422")
    void invalidState() throws Exception {
        when(alertService.acknowledge(1L)).thenThrow(new InvalidAlertStateException("bad"));

        mockMvc.perform(patch("/api/v1/alerts/1/acknowledge"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_ALERT_STATE"));
    }

    @Test
    @DisplayName("list by employee and badge")
    void nestedLists() throws Exception {
        when(alertService.findByEmployee(4L)).thenReturn(List.of(sample));
        when(alertService.findByBadge(3L)).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/v1/employees/4/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));

        mockMvc.perform(get("/api/v1/badges/3/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].badgeNumber").value("BDG-001"));
    }
}
