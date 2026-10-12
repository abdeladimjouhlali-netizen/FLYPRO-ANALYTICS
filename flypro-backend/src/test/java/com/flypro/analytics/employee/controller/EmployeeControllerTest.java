package com.flypro.analytics.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.common.exceptions.GlobalExceptionHandler;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.department.exception.DepartmentNotFoundException;
import com.flypro.analytics.employee.dto.EmployeeRequestDTO;
import com.flypro.analytics.employee.dto.EmployeeResponseDTO;
import com.flypro.analytics.employee.exception.DuplicateEmployeeEmailException;
import com.flypro.analytics.employee.exception.DuplicateEmployeeNumberException;
import com.flypro.analytics.employee.exception.EmployeeNotFoundException;
import com.flypro.analytics.employee.service.EmployeeService;
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

@WebMvcTest(controllers = EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;
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

    private final EmployeeResponseDTO sample = new EmployeeResponseDTO(
            10L, "EMP-001", "Sara", "Benali", "sara.benali@flypro.local",
            "+212612345678", "Analyst", EmployeeStatus.ACTIVE, null, null, null, null, null, null
    );

    private final String validBody = """
            {
              "employeeNumber": "EMP-001",
              "firstName": "Sara",
              "lastName": "Benali",
              "email": "sara.benali@flypro.local",
              "phone": "+212612345678",
              "position": "Analyst",
              "status": "ACTIVE",
              "departmentId": 1,
              "accessProfileId": null
            }
            """;

    @Test
    @DisplayName("POST create employee")
    void create() throws Exception {
        when(employeeService.create(any())).thenReturn(sample);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.employeeNumber").value("EMP-001"));
    }

    @Test
    @DisplayName("POST validation error")
    void createValidation() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeNumber": "",
                                  "firstName": "",
                                  "lastName": "Benali",
                                  "email": "bad",
                                  "position": "",
                                  "status": "ACTIVE",
                                  "departmentId": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST duplicate number")
    void createDuplicateNumber() throws Exception {
        when(employeeService.create(any())).thenThrow(new DuplicateEmployeeNumberException("EMP-001"));

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMPLOYEE_NUMBER"));
    }

    @Test
    @DisplayName("POST duplicate email")
    void createDuplicateEmail() throws Exception {
        when(employeeService.create(any())).thenThrow(new DuplicateEmployeeEmailException("sara.benali@flypro.local"));

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMPLOYEE_EMAIL"));
    }

    @Test
    @DisplayName("POST department not found")
    void createDepartmentNotFound() throws Exception {
        when(employeeService.create(any())).thenThrow(new DepartmentNotFoundException(99L));

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET by id")
    void getById() throws Exception {
        when(employeeService.getById(10L)).thenReturn(sample);

        mockMvc.perform(get("/api/v1/employees/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Sara"));
    }

    @Test
    @DisplayName("GET by id not found")
    void getByIdNotFound() throws Exception {
        when(employeeService.getById(99L)).thenThrow(new EmployeeNotFoundException(99L));

        mockMvc.perform(get("/api/v1/employees/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET search paginated")
    void search() throws Exception {
        when(employeeService.search(any(), any())).thenReturn(
                new PageResponse<>(List.of(sample), 0, 20, 1, 1, true, true)
        );

        mockMvc.perform(get("/api/v1/employees").param("keyword", "Sara"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("PUT update")
    void update() throws Exception {
        when(employeeService.update(eq(10L), any())).thenReturn(sample);

        mockMvc.perform(put("/api/v1/employees/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmployeeRequestDTO(
                                "EMP-001", "Sara", "Benali", "sara.benali@flypro.local",
                                "+212612345678", "Analyst", EmployeeStatus.ACTIVE, 1L, null
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Employee updated successfully"));
    }

    @Test
    @DisplayName("PATCH deactivate")
    void deactivate() throws Exception {
        when(employeeService.deactivate(10L)).thenReturn(sample);
        mockMvc.perform(patch("/api/v1/employees/10/deactivate"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH activate")
    void activate() throws Exception {
        when(employeeService.activate(10L)).thenReturn(sample);
        mockMvc.perform(patch("/api/v1/employees/10/activate"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH archive")
    void archive() throws Exception {
        when(employeeService.archive(10L)).thenReturn(sample);
        mockMvc.perform(patch("/api/v1/employees/10/archive"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE employee")
    void deleteEmployee() throws Exception {
        doNothing().when(employeeService).delete(10L);
        mockMvc.perform(delete("/api/v1/employees/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Employee deleted successfully"));
    }
}
