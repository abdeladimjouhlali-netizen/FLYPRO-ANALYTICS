package com.flypro.analytics.report.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flypro.analytics.report.dto.ExportFileResult;
import com.flypro.analytics.report.dto.ExportReportRequest;
import com.flypro.analytics.report.dto.ReportResponseDTO;
import com.flypro.analytics.report.dto.ReportScheduleResponse;
import com.flypro.analytics.report.dto.ScheduleReportRequest;
import com.flypro.analytics.report.enums.ExportFormat;
import com.flypro.analytics.report.enums.ReportFrequency;
import com.flypro.analytics.report.enums.ReportKind;
import com.flypro.analytics.report.enums.ReportType;
import com.flypro.analytics.report.service.ReportService;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.flypro.analytics.common.exceptions.GlobalExceptionHandler.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReportService reportService;
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
    @DisplayName("GET employees report returns JSON envelope")
    void employees() throws Exception {
        when(reportService.getReport(any(), any())).thenReturn(new ReportResponseDTO(
                ReportType.EMPLOYEES, ReportKind.DETAILED, "Employees Report", Instant.now(),
                Map.of("totalEmployees", 1), List.of("id"), List.of(Map.of("id", 1)), 1
        ));

        mockMvc.perform(get("/api/v1/reports/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Employees Report"));
    }

    @Test
    @DisplayName("POST export returns binary attachment")
    void export() throws Exception {
        when(reportService.export(any(ExportReportRequest.class)))
                .thenReturn(new ExportFileResult("report.pdf", "application/pdf", "%PDF".getBytes()));

        mockMvc.perform(post("/api/v1/reports/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportType": "EMPLOYEES",
                                  "exportFormat": "PDF",
                                  "reportKind": "DETAILED",
                                  "filters": {}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("report.pdf")));
    }

    @Test
    @DisplayName("POST schedule returns schedule DTO")
    void schedule() throws Exception {
        when(reportService.schedule(any(ScheduleReportRequest.class))).thenReturn(new ReportScheduleResponse(
                1L, "Weekly", ReportType.ACCESS_EVENTS, ExportFormat.EXCEL, ReportKind.SUMMARY,
                ReportFrequency.WEEKLY, List.of("a@b.c"), true, null, Instant.now(), Instant.now()
        ));

        mockMvc.perform(post("/api/v1/reports/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScheduleReportRequest(
                                "Weekly", ReportType.ACCESS_EVENTS, ExportFormat.EXCEL, ReportKind.SUMMARY,
                                ReportFrequency.WEEKLY, null, List.of("a@b.c")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }
}
