package com.flypro.analytics.dashboard;

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

import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private Long departmentId;
    private Long zoneId;
    private Long accessPointId;
    private Long employeeId;
    private Long badgeId;

    @BeforeEach
    void setUp() throws Exception {
        accessToken = login();
        departmentId = createDepartment();
        zoneId = createZone("Dashboard Zone", false);
        Long profileId = createAccessProfile(zoneId);
        employeeId = createEmployee(departmentId, profileId);
        accessPointId = createAccessPoint("Dashboard Door", "AP-DASH-01", zoneId);
        badgeId = createBadge("BDG-DASH-001", LocalDate.now().plusDays(10));
        mockMvc.perform(post("/api/v1/badges/{id}/assign/{employeeId}", badgeId, employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        createAccessEvent(Instant.parse("2026-07-20T08:00:00Z"), "AUTHORIZED");
        createAccessEvent(Instant.parse("2026-07-20T09:00:00Z"), "AUTHORIZED");
        createAccessEvent(Instant.parse("2026-07-21T14:00:00Z"), "DENIED");
        createZone("Sensitive Dash Zone", true);
    }

    @Test
    @DisplayName("KPIs reflect seeded inventory and events")
    void kpis() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/kpis")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-07-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.key=='totalEmployees')].value").value(org.hamcrest.Matchers.hasItem(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[?(@.key=='grantedAccesses')].value").value(org.hamcrest.Matchers.hasItem(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data[?(@.key=='deniedAccesses')].value").value(org.hamcrest.Matchers.hasItem(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[?(@.key=='sensitiveZones')].value").value(org.hamcrest.Matchers.hasItem(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("charts and statistics endpoints")
    void chartsAndStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/charts")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-07-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.key=='dailyAccessEvents')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.key=='grantedVsDenied')]").isNotEmpty());

        mockMvc.perform(get("/api/v1/dashboard/statistics")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-07-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topAccessPoints").isArray())
                .andExpect(jsonPath("$.data.mostActiveEmployees").isArray())
                .andExpect(jsonPath("$.data.badgesExpiringSoon").isArray());
    }

    @Test
    @DisplayName("filters by department and date range")
    void filters() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/kpis")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("departmentId", String.valueOf(departmentId))
                        .param("from", "2026-07-20T00:00:00Z")
                        .param("to", "2026-07-20T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.key=='totalAccessEvents')].value")
                        .value(org.hamcrest.Matchers.hasItem(greaterThanOrEqualTo(2))));

        mockMvc.perform(get("/api/v1/dashboard/kpis")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("from", "2026-07-22T00:00:00Z")
                        .param("to", "2026-07-20T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DASHBOARD_FILTER"));
    }

    @Test
    @DisplayName("top employees, zones, access points and peak hours")
    void topsAndPeakHours() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/top-access-points")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-07-31T23:59:59Z")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Dashboard Door"))
                .andExpect(jsonPath("$.data[0].count").value(greaterThanOrEqualTo(3)));

        mockMvc.perform(get("/api/v1/dashboard/top-zones")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-07-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Dashboard Zone"));

        mockMvc.perform(get("/api/v1/dashboard/top-employees")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-07-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].count").value(greaterThanOrEqualTo(3)));

        mockMvc.perform(get("/api/v1/dashboard/peak-hours")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-07-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(24)))
                .andExpect(jsonPath("$.data[8].count").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/dashboard/trends")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("from", "2026-07-15T00:00:00Z")
                        .param("to", "2026-07-22T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    private String login() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private void createAccessEvent(Instant ts, String result) throws Exception {
        mockMvc.perform(post("/api/v1/access-events")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventTimestamp": "%s",
                                  "direction": "ENTRY",
                                  "result": "%s",
                                  "source": "API",
                                  "badgeId": %d,
                                  "accessPointId": %d
                                }
                                """.formatted(ts, result, badgeId, accessPointId)))
                .andExpect(status().isCreated());
    }

    private Long createDepartment() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dash Dept","code":"DSH","description":"Dashboard dept"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private Long createZone(String name, boolean sensitive) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/zones")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "building": "Dash Building",
                                  "floor": "1",
                                  "sensitive": %s
                                }
                                """.formatted(name, sensitive)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private Long createAccessProfile(Long zone) throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/access-profiles")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Dash Profile %s",
                                  "description": "profile",
                                  "securityLevel": "MEDIUM",
                                  "startTime": "00:00:00",
                                  "endTime": "23:59:59",
                                  "zoneIds": [%d]
                                }
                                """.formatted(System.nanoTime(), zone)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(create.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private Long createEmployee(Long deptId, Long profileId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeNumber": "EMP-DASH-1",
                                  "firstName": "Dash",
                                  "lastName": "User",
                                  "email": "dash.user@flypro.local",
                                  "position": "Analyst",
                                  "status": "ACTIVE",
                                  "departmentId": %d,
                                  "accessProfileId": %d
                                }
                                """.formatted(deptId, profileId)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private Long createAccessPoint(String name, String code, Long zone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/access-points")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "code": "%s",
                                  "type": "DOOR",
                                  "status": "ACTIVE",
                                  "zoneId": %d
                                }
                                """.formatted(name, code, zone)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private Long createBadge(String number, LocalDate expiration) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/badges")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "badgeNumber": "%s",
                                  "type": "RFID",
                                  "status": "ACTIVE",
                                  "issueDate": "%s",
                                  "expirationDate": "%s"
                                }
                                """.formatted(number, LocalDate.now().minusDays(5), expiration)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }
}
