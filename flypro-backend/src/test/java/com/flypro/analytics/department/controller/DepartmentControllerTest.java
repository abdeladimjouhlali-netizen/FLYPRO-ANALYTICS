package com.flypro.analytics.department.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.department.dto.DepartmentRequest;
import com.flypro.analytics.department.dto.DepartmentResponse;
import com.flypro.analytics.department.exception.DepartmentNotFoundException;
import com.flypro.analytics.department.exception.DuplicateDepartmentCodeException;
import com.flypro.analytics.department.service.DepartmentService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DepartmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.flypro.analytics.common.exceptions.GlobalExceptionHandler.class)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DepartmentService departmentService;

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

    private final DepartmentResponse sample = new DepartmentResponse(
            1L, "Human Resources", "HR", "HR dept", true,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z")
    );

    @Test
    @DisplayName("POST creates department")
    void create() throws Exception {
        when(departmentService.create(any(DepartmentRequest.class))).thenReturn(sample);

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Human Resources",
                                  "code": "HR",
                                  "description": "HR dept"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("HR"));
    }

    @Test
    @DisplayName("POST validation rejects blank name")
    void createValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "code": "HR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST duplicate code returns conflict")
    void createDuplicateCode() throws Exception {
        when(departmentService.create(any())).thenThrow(new DuplicateDepartmentCodeException("HR"));

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Human Resources",
                                  "code": "HR"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_DEPARTMENT_CODE"));
    }

    @Test
    @DisplayName("GET by id returns department")
    void getById() throws Exception {
        when(departmentService.getById(1L)).thenReturn(sample);

        mockMvc.perform(get("/api/v1/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Human Resources"));
    }

    @Test
    @DisplayName("GET by id not found")
    void getByIdNotFound() throws Exception {
        when(departmentService.getById(99L)).thenThrow(new DepartmentNotFoundException(99L));

        mockMvc.perform(get("/api/v1/departments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET list supports pagination payload")
    void search() throws Exception {
        PageResponse<DepartmentResponse> page = new PageResponse<>(
                List.of(sample), 0, 20, 1, 1, true, true
        );
        when(departmentService.search(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/departments")
                        .param("name", "Human")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].code").value("HR"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("PUT updates department")
    void update() throws Exception {
        when(departmentService.update(eq(1L), any())).thenReturn(sample);

        mockMvc.perform(put("/api/v1/departments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DepartmentRequest("Human Resources", "HR", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Department updated successfully"));
    }

    @Test
    @DisplayName("DELETE hard delete returns message only")
    void deleteHard() throws Exception {
        when(departmentService.delete(1L)).thenReturn(null);

        mockMvc.perform(delete("/api/v1/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Department deleted successfully"));
    }

    @Test
    @DisplayName("DELETE soft delete returns deactivated department")
    void deleteSoft() throws Exception {
        DepartmentResponse deactivated = new DepartmentResponse(
                1L, "Human Resources", "HR", "HR dept", false,
                sample.createdAt(), sample.updatedAt()
        );
        when(departmentService.delete(1L)).thenReturn(deactivated);

        mockMvc.perform(delete("/api/v1/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }
}
