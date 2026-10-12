package com.flypro.analytics.accesspoint.controller;

import com.flypro.analytics.accesspoint.dto.AccessPointResponseDTO;
import com.flypro.analytics.accesspoint.exception.AccessPointNotFoundException;
import com.flypro.analytics.accesspoint.exception.DuplicateAccessPointCodeException;
import com.flypro.analytics.accesspoint.exception.DuplicateAccessPointNameException;
import com.flypro.analytics.accesspoint.exception.InvalidIpAddressException;
import com.flypro.analytics.accesspoint.service.AccessPointService;
import com.flypro.analytics.common.enums.AccessPointStatus;
import com.flypro.analytics.common.enums.AccessPointType;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AccessPointController.class, ZoneAccessPointController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AccessPointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessPointService accessPointService;
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

    private final AccessPointResponseDTO sample = new AccessPointResponseDTO(
            10L, "Main Entrance Door", "AP-MAIN-01", AccessPointType.DOOR,
            "Primary entrance", AccessPointStatus.ACTIVE,
            "192.168.1.10", "Lobby", null, null, null
    );

    private final String validBody = """
            {
              "name": "Main Entrance Door",
              "code": "AP-MAIN-01",
              "type": "DOOR",
              "description": "Primary entrance",
              "status": "ACTIVE",
              "ipAddress": "192.168.1.10",
              "location": "Lobby",
              "zoneId": 1
            }
            """;

    @Test
    @DisplayName("POST create access point")
    void create() throws Exception {
        when(accessPointService.create(any())).thenReturn(sample);

        mockMvc.perform(post("/api/v1/access-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("AP-MAIN-01"));
    }

    @Test
    @DisplayName("POST validation error")
    void createValidation() throws Exception {
        mockMvc.perform(post("/api/v1/access-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","code":"","type":null,"status":null,"zoneId":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST duplicate code / name / zone / ip")
    void createErrors() throws Exception {
        doThrow(new DuplicateAccessPointCodeException("AP-MAIN-01"))
                .when(accessPointService).create(any());
        mockMvc.perform(post("/api/v1/access-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ACCESS_POINT_CODE"));

        doThrow(new DuplicateAccessPointNameException("Main Entrance Door"))
                .when(accessPointService).create(any());
        mockMvc.perform(post("/api/v1/access-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ACCESS_POINT_NAME"));

        doThrow(new ZoneNotFoundException(99L))
                .when(accessPointService).create(any());
        mockMvc.perform(post("/api/v1/access-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ZONE_NOT_FOUND"));

        doThrow(new InvalidIpAddressException("bad"))
                .when(accessPointService).create(any());
        mockMvc.perform(post("/api/v1/access-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IP_ADDRESS"));
    }

    @Test
    @DisplayName("GET / PUT / DELETE / PATCH status / zone listing")
    void operations() throws Exception {
        when(accessPointService.getById(10L)).thenReturn(sample);
        when(accessPointService.search(any(), any())).thenReturn(
                new PageResponse<>(List.of(sample), 0, 20, 1, 1, true, true)
        );
        when(accessPointService.update(eq(10L), any())).thenReturn(sample);
        doNothing().when(accessPointService).delete(10L);
        when(accessPointService.activate(10L)).thenReturn(sample);
        when(accessPointService.deactivate(10L)).thenReturn(sample);
        when(accessPointService.putInMaintenance(10L)).thenReturn(sample);
        when(accessPointService.listByZone(1L)).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/v1/access-points/10")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/access-points").param("type", "DOOR")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(put("/api/v1/access-points/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/access-points/10")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/access-points/10/activate")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/access-points/10/deactivate")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/access-points/10/maintenance")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/zones/1/access-points")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("AP-MAIN-01"));
    }

    @Test
    @DisplayName("GET not found")
    void notFound() throws Exception {
        when(accessPointService.getById(99L)).thenThrow(new AccessPointNotFoundException(99L));

        mockMvc.perform(get("/api/v1/access-points/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCESS_POINT_NOT_FOUND"));
    }
}
