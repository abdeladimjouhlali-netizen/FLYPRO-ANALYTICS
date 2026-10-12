package com.flypro.analytics.zone.controller;

import com.flypro.analytics.common.exceptions.GlobalExceptionHandler;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.security.handler.SecurityExceptionHandler;
import com.flypro.analytics.security.jwt.JwtAuthenticationFilter;
import com.flypro.analytics.infra.security.ApiKeyAuthFilter;
import com.flypro.analytics.infra.security.JwtBlacklistFilter;
import com.flypro.analytics.security.jwt.JwtService;
import com.flypro.analytics.security.userdetails.CustomUserDetailsService;
import com.flypro.analytics.zone.dto.ZoneResponseDTO;
import com.flypro.analytics.zone.exception.DuplicateZoneException;
import com.flypro.analytics.zone.exception.ZoneNotFoundException;
import com.flypro.analytics.zone.service.ZoneService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ZoneController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ZoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZoneService zoneService;
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

    private final ZoneResponseDTO sample = new ZoneResponseDTO(
            1L, "Server Room A", "Building North", "3", true, "Restricted", null, null
    );

    private final String validBody = """
            {
              "name": "Server Room A",
              "building": "Building North",
              "floor": "3",
              "sensitive": true,
              "description": "Restricted"
            }
            """;

    @Test
    @DisplayName("POST create zone")
    void create() throws Exception {
        when(zoneService.create(any())).thenReturn(sample);

        mockMvc.perform(post("/api/v1/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Server Room A"))
                .andExpect(jsonPath("$.data.sensitive").value(true));
    }

    @Test
    @DisplayName("POST validation error")
    void createValidation() throws Exception {
        mockMvc.perform(post("/api/v1/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","building":"","floor":"","sensitive":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST duplicate zone")
    void createDuplicate() throws Exception {
        when(zoneService.create(any())).thenThrow(new DuplicateZoneException("Server Room A"));

        mockMvc.perform(post("/api/v1/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ZONE"));
    }

    @Test
    @DisplayName("GET by id / search / update / delete")
    void crud() throws Exception {
        when(zoneService.getById(1L)).thenReturn(sample);
        when(zoneService.search(any(), any())).thenReturn(
                new PageResponse<>(List.of(sample), 0, 20, 1, 1, true, true)
        );
        when(zoneService.update(eq(1L), any())).thenReturn(sample);
        doNothing().when(zoneService).delete(1L);

        mockMvc.perform(get("/api/v1/zones/1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/zones").param("sensitive", "true")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(put("/api/v1/zones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/zones/1")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET not found")
    void notFound() throws Exception {
        when(zoneService.getById(99L)).thenThrow(new ZoneNotFoundException(99L));

        mockMvc.perform(get("/api/v1/zones/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ZONE_NOT_FOUND"));
    }
}
