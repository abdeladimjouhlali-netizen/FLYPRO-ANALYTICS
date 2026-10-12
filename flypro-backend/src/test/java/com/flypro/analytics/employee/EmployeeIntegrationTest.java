package com.flypro.analytics.employee;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for the Employee module (auth + persistence).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmployeeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private Long departmentId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        accessToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        MvcResult dept = mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Integration Dept",
                                  "code": "INTD",
                                  "description": "Integration department"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        departmentId = objectMapper.readTree(dept.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    @DisplayName("full employee lifecycle: create, search, archive, delete")
    void employeeLifecycle() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeNumber": "EMP-INT-1",
                                  "firstName": "Sara",
                                  "lastName": "Benali",
                                  "email": "sara.int@flypro.local",
                                  "phone": "+212612345678",
                                  "position": "Analyst",
                                  "status": "ACTIVE",
                                  "departmentId": %d,
                                  "accessProfileId": null
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        long employeeId = objectMapper.readTree(create.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("keyword", "Sara")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(patch("/api/v1/employees/{id}/deactivate", employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(patch("/api/v1/employees/{id}/activate", employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/v1/employees/{id}/archive", employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        mockMvc.perform(delete("/api/v1/employees/{id}", employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/employees/{id}", employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("duplicate employee number is rejected")
    void duplicateNumber() throws Exception {
        String body = """
                {
                  "employeeNumber": "EMP-DUP",
                  "firstName": "A",
                  "lastName": "B",
                  "email": "a.b@flypro.local",
                  "position": "Analyst",
                  "status": "ACTIVE",
                  "departmentId": %d
                }
                """.formatted(departmentId);

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        String duplicate = """
                {
                  "employeeNumber": "EMP-DUP",
                  "firstName": "C",
                  "lastName": "D",
                  "email": "c.d@flypro.local",
                  "position": "Analyst",
                  "status": "ACTIVE",
                  "departmentId": %d
                }
                """.formatted(departmentId);

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicate))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMPLOYEE_NUMBER"));
    }

    @Test
    @DisplayName("unauthorized without token")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isUnauthorized());
    }
}
