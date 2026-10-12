package com.flypro.analytics.accessprofile.controller;

import com.flypro.analytics.accessprofile.dto.AccessProfileResponseDTO;
import com.flypro.analytics.accessprofile.exception.DuplicateAccessProfileException;
import com.flypro.analytics.accessprofile.exception.InvalidWorkingHoursException;
import com.flypro.analytics.accessprofile.exception.ZoneAlreadyAssignedException;
import com.flypro.analytics.accessprofile.service.AccessProfileService;
import com.flypro.analytics.common.enums.SecurityLevel;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

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

@WebMvcTest(controllers = AccessProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AccessProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessProfileService accessProfileService;
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

    private final AccessProfileResponseDTO sample = new AccessProfileResponseDTO(
            10L, "Standard", "Office", SecurityLevel.MEDIUM,
            LocalTime.of(8, 0), LocalTime.of(18, 0), Set.of(), null, null
    );

    private final String validBody = """
            {
              "name": "Standard",
              "description": "Office",
              "securityLevel": "MEDIUM",
              "startTime": "08:00:00",
              "endTime": "18:00:00",
              "zoneIds": [1]
            }
            """;

    @Test
    @DisplayName("POST create access profile")
    void create() throws Exception {
        when(accessProfileService.create(any())).thenReturn(sample);

        mockMvc.perform(post("/api/v1/access-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Standard"));
    }

    @Test
    @DisplayName("POST validation / duplicate / invalid hours")
    void createErrors() throws Exception {
        mockMvc.perform(post("/api/v1/access-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "securityLevel": "MEDIUM",
                                  "startTime": "08:00:00",
                                  "endTime": "18:00:00",
                                  "zoneIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        doThrow(new DuplicateAccessProfileException("Standard"))
                .when(accessProfileService).create(any());
        mockMvc.perform(post("/api/v1/access-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ACCESS_PROFILE"));

        doThrow(new InvalidWorkingHoursException("Start time must be before end time"))
                .when(accessProfileService).create(any());
        mockMvc.perform(post("/api/v1/access-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WORKING_HOURS"));
    }

    @Test
    @DisplayName("search / get / update / delete / assign / remove zone")
    void operations() throws Exception {
        when(accessProfileService.search(any(), any())).thenReturn(
                new PageResponse<>(List.of(sample), 0, 20, 1, 1, true, true)
        );
        when(accessProfileService.getById(10L)).thenReturn(sample);
        when(accessProfileService.update(eq(10L), any())).thenReturn(sample);
        doNothing().when(accessProfileService).delete(10L);
        when(accessProfileService.assignZone(10L, 2L)).thenReturn(sample);
        when(accessProfileService.removeZone(10L, 2L)).thenReturn(sample);

        mockMvc.perform(get("/api/v1/access-profiles").param("securityLevel", "MEDIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/v1/access-profiles/10")).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/access-profiles/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/access-profiles/10")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/access-profiles/10/zones/2")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/access-profiles/10/zones/2")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("assign already assigned zone")
    void assignConflict() throws Exception {
        when(accessProfileService.assignZone(10L, 1L))
                .thenThrow(new ZoneAlreadyAssignedException(10L, 1L));

        mockMvc.perform(post("/api/v1/access-profiles/10/zones/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ZONE_ALREADY_ASSIGNED"));
    }
}
