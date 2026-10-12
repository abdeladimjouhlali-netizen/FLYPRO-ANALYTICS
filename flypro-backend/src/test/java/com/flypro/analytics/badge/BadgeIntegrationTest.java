package com.flypro.analytics.badge;

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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for the Badge module (auth + persistence).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BadgeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private Long departmentId;
    private Long employeeId;

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
                                  "name": "Badge Dept",
                                  "code": "BDGD",
                                  "description": "Badge integration department"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        departmentId = objectMapper.readTree(dept.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        MvcResult emp = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeNumber": "EMP-BDG-1",
                                  "firstName": "Sara",
                                  "lastName": "Benali",
                                  "email": "sara.badge@flypro.local",
                                  "phone": "+212612345678",
                                  "position": "Analyst",
                                  "status": "ACTIVE",
                                  "departmentId": %d,
                                  "accessProfileId": null
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isCreated())
                .andReturn();

        employeeId = objectMapper.readTree(emp.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    @DisplayName("full badge lifecycle: create, assign, suspend, activate, renew, history, revoke")
    void badgeLifecycle() throws Exception {
        String issue = LocalDate.now().minusDays(10).toString();
        String expiration = LocalDate.now().plusYears(1).toString();

        MvcResult create = mockMvc.perform(post("/api/v1/badges")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "badgeNumber": "BDG-INT-1",
                                  "type": "RFID",
                                  "status": "PENDING_ACTIVATION",
                                  "issueDate": "%s",
                                  "expirationDate": "%s"
                                }
                                """.formatted(issue, expiration)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_ACTIVATION"))
                .andReturn();

        long badgeId = objectMapper.readTree(create.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/badges/{id}/assign/{employeeId}", badgeId, employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.currentAssignment.employeeId").value(employeeId));

        mockMvc.perform(get("/api/v1/badges")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("keyword", "BDG-INT")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(patch("/api/v1/badges/{id}/suspend", badgeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        mockMvc.perform(patch("/api/v1/badges/{id}/activate", badgeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        String newExpiration = LocalDate.now().plusYears(2).toString();
        mockMvc.perform(patch("/api/v1/badges/{id}/renew", badgeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expirationDate":"%s","issueDate":"%s"}
                                """.formatted(newExpiration, LocalDate.now().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expirationDate").value(newExpiration));

        mockMvc.perform(get("/api/v1/badges/{id}/history", badgeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/v1/employees/{employeeId}/badges", employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(patch("/api/v1/badges/{id}/revoke", badgeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVOKED"));
    }

    @Test
    @DisplayName("replace badge transfers assignment and keeps history")
    void replaceBadge() throws Exception {
        String issue = LocalDate.now().minusDays(5).toString();
        String expiration = LocalDate.now().plusYears(1).toString();

        long oldId = createBadge("BDG-OLD", issue, expiration);
        long newId = createBadge("BDG-NEW", issue, expiration);

        mockMvc.perform(post("/api/v1/badges/{id}/assign/{employeeId}", oldId, employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/badges/{id}/replace", oldId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newBadgeId":%d}
                                """.formatted(newId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(newId))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/badges/{id}/history", oldId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].active").value(false));
    }

    @Test
    @DisplayName("duplicate badge number is rejected")
    void duplicateNumber() throws Exception {
        String body = """
                {
                  "badgeNumber": "BDG-DUP",
                  "type": "NFC",
                  "status": "PENDING_ACTIVATION",
                  "issueDate": "%s",
                  "expirationDate": "%s"
                }
                """.formatted(LocalDate.now().toString(), LocalDate.now().plusYears(1).toString());

        mockMvc.perform(post("/api/v1/badges")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/badges")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_BADGE_NUMBER"));
    }

    @Test
    @DisplayName("cannot assign badge to inactive employee")
    void inactiveEmployee() throws Exception {
        mockMvc.perform(patch("/api/v1/employees/{id}/deactivate", employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        long badgeId = createBadge(
                "BDG-INA",
                LocalDate.now().toString(),
                LocalDate.now().plusYears(1).toString()
        );

        mockMvc.perform(post("/api/v1/badges/{id}/assign/{employeeId}", badgeId, employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("EMPLOYEE_INACTIVE"));
    }

    @Test
    @DisplayName("delete allowed only without assignment history")
    void deleteRules() throws Exception {
        long deletable = createBadge(
                "BDG-DEL",
                LocalDate.now().toString(),
                LocalDate.now().plusYears(1).toString()
        );

        mockMvc.perform(delete("/api/v1/badges/{id}", deletable)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        long assigned = createBadge(
                "BDG-KEEP",
                LocalDate.now().toString(),
                LocalDate.now().plusYears(1).toString()
        );
        mockMvc.perform(post("/api/v1/badges/{id}/assign/{employeeId}", assigned, employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/badges/{id}", assigned)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BADGE_CANNOT_BE_DELETED"));
    }

    @Test
    @DisplayName("unauthorized without token")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/badges"))
                .andExpect(status().isUnauthorized());
    }

    private long createBadge(String number, String issue, String expiration) throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/badges")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "badgeNumber": "%s",
                                  "type": "RFID",
                                  "status": "PENDING_ACTIVATION",
                                  "issueDate": "%s",
                                  "expirationDate": "%s"
                                }
                                """.formatted(number, issue, expiration)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(create.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }
}
