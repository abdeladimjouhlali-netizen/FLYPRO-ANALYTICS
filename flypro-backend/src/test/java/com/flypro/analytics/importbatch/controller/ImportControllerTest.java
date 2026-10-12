package com.flypro.analytics.importbatch.controller;

import com.flypro.analytics.common.enums.ImportFileType;
import com.flypro.analytics.common.enums.ImportStatus;
import com.flypro.analytics.common.exceptions.GlobalExceptionHandler;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.importbatch.dto.ImportResponseDTO;
import com.flypro.analytics.importbatch.dto.ImportSummaryDTO;
import com.flypro.analytics.importbatch.exception.ImportBatchNotFoundException;
import com.flypro.analytics.importbatch.exception.UnsupportedFileTypeException;
import com.flypro.analytics.importbatch.service.ImportService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportService importService;
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

    private final ImportResponseDTO sample = new ImportResponseDTO(
            1L, "events.csv", ImportFileType.CSV, ImportStatus.PENDING,
            0, 0, 0, 0, null, null, null, "admin", List.of()
    );

    @Test
    @DisplayName("POST upload returns 201")
    void upload() throws Exception {
        when(importService.upload(any())).thenReturn(sample);
        MockMultipartFile file = new MockMultipartFile(
                "file", "events.csv", "text/csv", "a,b\n1,2".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/imports/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST preview returns summary")
    void preview() throws Exception {
        ImportSummaryDTO summary = new ImportSummaryDTO(
                null, "events.csv", ImportFileType.CSV, ImportStatus.PENDING,
                10, 8, 1, 1, List.of(), List.of(), 12L
        );
        when(importService.preview(any(org.springframework.web.multipart.MultipartFile.class))).thenReturn(summary);
        MockMultipartFile file = new MockMultipartFile(
                "file", "events.csv", "text/csv", "a,b\n1,2".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/imports/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRecords").value(10))
                .andExpect(jsonPath("$.data.importedRecords").value(8));
    }

    @Test
    @DisplayName("POST execute with batchId")
    void execute() throws Exception {
        ImportResponseDTO completed = new ImportResponseDTO(
                1L, "events.csv", ImportFileType.CSV, ImportStatus.COMPLETED,
                10, 10, 0, 0, null, null, null, "admin", List.of()
        );
        when(importService.execute(any(com.flypro.analytics.importbatch.dto.ImportRequestDTO.class))).thenReturn(completed);

        mockMvc.perform(post("/api/v1/imports/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET imports list")
    void list() throws Exception {
        when(importService.search(any(), any())).thenReturn(PageResponse.from(
                new org.springframework.data.domain.PageImpl<>(List.of(sample))));

        mockMvc.perform(get("/api/v1/imports").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].fileName").value("events.csv"));
    }

    @Test
    @DisplayName("GET import by id not found")
    void notFound() throws Exception {
        when(importService.getById(99L)).thenThrow(new ImportBatchNotFoundException(99L));

        mockMvc.perform(get("/api/v1/imports/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("IMPORT_BATCH_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET report")
    void report() throws Exception {
        ImportSummaryDTO summary = new ImportSummaryDTO(
                1L, "events.csv", ImportFileType.CSV, ImportStatus.COMPLETED,
                10, 9, 1, 0, List.of(), List.of(), 100L
        );
        when(importService.getReport(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/imports/1/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processingTimeMs").value(100));
    }

    @Test
    @DisplayName("unsupported file type maps to 400")
    void unsupportedType() throws Exception {
        when(importService.upload(any())).thenThrow(new UnsupportedFileTypeException("events.txt"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "events.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/imports/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
    }
}
