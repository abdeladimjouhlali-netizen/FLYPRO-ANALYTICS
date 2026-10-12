package com.flypro.analytics.dashboard.controller;

import com.flypro.analytics.common.exceptions.GlobalExceptionHandler;
import com.flypro.analytics.dashboard.dto.ChartDTO;
import com.flypro.analytics.dashboard.dto.ChartSeriesDTO;
import com.flypro.analytics.dashboard.dto.KpiDTO;
import com.flypro.analytics.dashboard.dto.NamedCountDTO;
import com.flypro.analytics.dashboard.dto.StatisticsDTO;
import com.flypro.analytics.dashboard.dto.TrendDTO;
import com.flypro.analytics.dashboard.exception.InvalidDashboardFilterException;
import com.flypro.analytics.dashboard.service.DashboardService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;
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

    @Test
    @DisplayName("GET /kpis")
    void kpis() throws Exception {
        when(dashboardService.getKpis(any())).thenReturn(List.of(
                new KpiDTO("totalEmployees", "Total Employees", 10, "count")
        ));

        mockMvc.perform(get("/api/v1/dashboard/kpis").param("departmentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].key").value("totalEmployees"))
                .andExpect(jsonPath("$.data[0].value").value(10));
    }

    @Test
    @DisplayName("GET /charts")
    void charts() throws Exception {
        when(dashboardService.getCharts(any())).thenReturn(List.of(
                new ChartDTO("dailyAccessEvents", "Daily Access Events",
                        List.of("2026-07-20"), List.of(new ChartSeriesDTO("count", List.of(5L))))
        ));

        mockMvc.perform(get("/api/v1/dashboard/charts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].key").value("dailyAccessEvents"));
    }

    @Test
    @DisplayName("GET /statistics")
    void statistics() throws Exception {
        when(dashboardService.getStatistics(any())).thenReturn(new StatisticsDTO(
                List.of(new NamedCountDTO(1L, "Door", 9L)),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        ));

        mockMvc.perform(get("/api/v1/dashboard/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topAccessPoints[0].name").value("Door"));
    }

    @Test
    @DisplayName("GET /trends")
    void trends() throws Exception {
        when(dashboardService.getTrends(any())).thenReturn(List.of(
                new TrendDTO("accessEvents", "Access Events", 20, 10, 100.0)
        ));

        mockMvc.perform(get("/api/v1/dashboard/trends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].changePercent").value(100.0));
    }

    @Test
    @DisplayName("GET top endpoints")
    void tops() throws Exception {
        when(dashboardService.getTopAccessPoints(any(), eq(10)))
                .thenReturn(List.of(new NamedCountDTO(1L, "AP", 5L)));
        when(dashboardService.getTopZones(any(), anyInt()))
                .thenReturn(List.of(new NamedCountDTO(2L, "Zone", 4L)));
        when(dashboardService.getTopEmployees(any(), anyInt()))
                .thenReturn(List.of(new NamedCountDTO(3L, "Emp", 3L)));

        mockMvc.perform(get("/api/v1/dashboard/top-access-points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("AP"));
        mockMvc.perform(get("/api/v1/dashboard/top-zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Zone"));
        mockMvc.perform(get("/api/v1/dashboard/top-employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Emp"));
    }

    @Test
    @DisplayName("GET /peak-hours")
    void peakHours() throws Exception {
        when(dashboardService.getPeakHours(any())).thenReturn(List.of(
                new NamedCountDTO(8L, "08:00", 2L)
        ));

        mockMvc.perform(get("/api/v1/dashboard/peak-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].count").value(2));
    }

    @Test
    @DisplayName("invalid filter returns 400")
    void invalidFilter() throws Exception {
        when(dashboardService.getKpis(any())).thenThrow(new InvalidDashboardFilterException("'from' must be before or equal to 'to'"));

        mockMvc.perform(get("/api/v1/dashboard/kpis")
                        .param("from", "2026-07-22T00:00:00Z")
                        .param("to", "2026-07-20T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DASHBOARD_FILTER"));
    }
}
