package com.flypro.analytics.badge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flypro.analytics.badge.dto.BadgeAssignmentDTO;
import com.flypro.analytics.badge.dto.BadgeResponseDTO;
import com.flypro.analytics.badge.exception.BadgeAlreadyAssignedException;
import com.flypro.analytics.badge.exception.BadgeNotFoundException;
import com.flypro.analytics.badge.exception.DuplicateBadgeNumberException;
import com.flypro.analytics.badge.exception.EmployeeInactiveException;
import com.flypro.analytics.badge.service.BadgeService;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.BadgeType;
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

import java.time.LocalDate;
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

@WebMvcTest(controllers = {BadgeController.class, EmployeeBadgeController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BadgeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BadgeService badgeService;
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

    private final BadgeResponseDTO sample = new BadgeResponseDTO(
            1L, "BDG-001", BadgeType.RFID, BadgeStatus.PENDING_ACTIVATION,
            LocalDate.of(2026, 1, 15), LocalDate.of(2027, 1, 15), null, null, null
    );

    private final String validBody = """
            {
              "badgeNumber": "BDG-001",
              "type": "RFID",
              "status": "PENDING_ACTIVATION",
              "issueDate": "2026-01-15",
              "expirationDate": "2027-01-15"
            }
            """;

    @Test
    @DisplayName("POST create badge")
    void create() throws Exception {
        when(badgeService.create(any())).thenReturn(sample);

        mockMvc.perform(post("/api/v1/badges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.badgeNumber").value("BDG-001"));
    }

    @Test
    @DisplayName("POST validation error")
    void createValidation() throws Exception {
        mockMvc.perform(post("/api/v1/badges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "badgeNumber": "",
                                  "type": "RFID",
                                  "status": "ACTIVE",
                                  "issueDate": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST duplicate badge number")
    void createDuplicate() throws Exception {
        when(badgeService.create(any())).thenThrow(new DuplicateBadgeNumberException("BDG-001"));

        mockMvc.perform(post("/api/v1/badges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_BADGE_NUMBER"));
    }

    @Test
    @DisplayName("GET badge by id")
    void getById() throws Exception {
        when(badgeService.getById(1L)).thenReturn(sample);

        mockMvc.perform(get("/api/v1/badges/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("GET badge not found")
    void getNotFound() throws Exception {
        when(badgeService.getById(99L)).thenThrow(new BadgeNotFoundException(99L));

        mockMvc.perform(get("/api/v1/badges/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BADGE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET search badges")
    void search() throws Exception {
        when(badgeService.search(any(), any())).thenReturn(
                new PageResponse<>(List.of(sample), 0, 20, 1, 1, true, true)
        );

        mockMvc.perform(get("/api/v1/badges")
                        .param("status", "PENDING_ACTIVATION")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("PUT update badge")
    void update() throws Exception {
        when(badgeService.update(eq(1L), any())).thenReturn(sample);

        mockMvc.perform(put("/api/v1/badges/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST assign badge")
    void assign() throws Exception {
        when(badgeService.assign(1L, 10L)).thenReturn(sample);

        mockMvc.perform(post("/api/v1/badges/1/assign/10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST assign inactive employee")
    void assignInactive() throws Exception {
        when(badgeService.assign(1L, 10L)).thenThrow(new EmployeeInactiveException(10L));

        mockMvc.perform(post("/api/v1/badges/1/assign/10"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("EMPLOYEE_INACTIVE"));
    }

    @Test
    @DisplayName("POST assign already assigned")
    void assignConflict() throws Exception {
        when(badgeService.assign(1L, 10L)).thenThrow(new BadgeAlreadyAssignedException(1L));

        mockMvc.perform(post("/api/v1/badges/1/assign/10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BADGE_ALREADY_ASSIGNED"));
    }

    @Test
    @DisplayName("PATCH suspend / activate / revoke / lost / unassign / renew / replace")
    void statusTransitions() throws Exception {
        when(badgeService.suspend(1L)).thenReturn(sample);
        when(badgeService.activate(1L)).thenReturn(sample);
        when(badgeService.revoke(1L)).thenReturn(sample);
        when(badgeService.reportLost(1L)).thenReturn(sample);
        when(badgeService.unassign(1L)).thenReturn(sample);
        when(badgeService.renew(eq(1L), any())).thenReturn(sample);
        when(badgeService.replace(eq(1L), any())).thenReturn(sample);

        mockMvc.perform(patch("/api/v1/badges/1/suspend")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/badges/1/activate")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/badges/1/revoke")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/badges/1/lost")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/badges/1/unassign")).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/badges/1/renew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expirationDate":"2028-01-15","issueDate":"2026-07-24"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/badges/1/replace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newBadgeId":2}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET history")
    void history() throws Exception {
        when(badgeService.getHistory(1L)).thenReturn(List.of(
                new BadgeAssignmentDTO(1L, 1L, "BDG-001", 10L, "EMP-001", "Sara", "Benali",
                        null, null, true)
        ));

        mockMvc.perform(get("/api/v1/badges/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].employeeNumber").value("EMP-001"));
    }

    @Test
    @DisplayName("GET employee badges")
    void employeeBadges() throws Exception {
        when(badgeService.listEmployeeBadges(10L)).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/v1/employees/10/badges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].badgeNumber").value("BDG-001"));
    }

    @Test
    @DisplayName("DELETE badge")
    void deleteBadge() throws Exception {
        doNothing().when(badgeService).delete(1L);

        mockMvc.perform(delete("/api/v1/badges/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Badge deleted successfully"));
    }

    @Test
    @DisplayName("DELETE not found")
    void deleteNotFound() throws Exception {
        doThrow(new BadgeNotFoundException(99L)).when(badgeService).delete(99L);

        mockMvc.perform(delete("/api/v1/badges/99"))
                .andExpect(status().isNotFound());
    }
}
