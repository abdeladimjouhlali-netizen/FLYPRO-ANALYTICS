package com.flypro.analytics.alert;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AlertIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private Long zoneId;
    private Long otherZoneId;
    private Long accessPointId;
    private Long badgeId;
    private Long employeeId;
    private Long departmentId;

    @BeforeEach
    void setUp() throws Exception {
        accessToken = login();

        departmentId = createDepartment();
        zoneId = createZone("Alert Zone A");
        otherZoneId = createZone("Alert Zone B");
        Long profileId = createAccessProfile(zoneId);
        employeeId = createEmployee(departmentId, profileId);
        accessPointId = createAccessPoint("Alert Door", "AP-ALERT-01", zoneId);
        badgeId = createBadge("BDG-ALERT-001");
        mockMvc.perform(post("/api/v1/badges/{id}/assign/{employeeId}", badgeId, employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("access denied and badge revoked generate alerts; resolve works")
    void accessDeniedAndResolve() throws Exception {
        // Force revoke then access
        mockMvc.perform(patch("/api/v1/badges/{id}/revoke", badgeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        createAccessEvent(Instant.parse("2026-07-26T10:00:00Z"), badgeId, "AUTHORIZED");

        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("type", "BADGE_REVOKED")
                        .param("badgeId", String.valueOf(badgeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        MvcResult list = mockMvc.perform(get("/api/v1/badges/{badgeId}/alerts", badgeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        long alertId = objectMapper.readTree(list.getResponse().getContentAsString())
                .path("data").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/v1/alerts/{id}/acknowledge", alertId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));

        mockMvc.perform(patch("/api/v1/alerts/{id}/resolve", alertId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolvedBy").value("admin"));
    }

    @Test
    @DisplayName("unauthorized zone and outside hours alerts")
    void policyAlerts() throws Exception {
        Long foreignAp = createAccessPoint("Foreign Door", "AP-ALERT-99", otherZoneId);

        mockMvc.perform(post("/api/v1/access-events")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventTimestamp": "2026-07-26T22:30:00Z",
                                  "direction": "ENTRY",
                                  "result": "AUTHORIZED",
                                  "source": "API",
                                  "badgeId": %d,
                                  "accessPointId": %d
                                }
                                """.formatted(badgeId, foreignAp)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("employeeId", String.valueOf(employeeId))
                        .param("type", "UNAUTHORIZED_ZONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("type", "OUTSIDE_WORKING_HOURS")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("access denied alert filtering pagination and close")
    void accessDeniedFilterAndClose() throws Exception {
        createAccessEvent(Instant.parse("2026-07-26T13:00:00Z"), badgeId, "DENIED");

        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("type", "ACCESS_DENIED")
                        .param("badgeId", String.valueOf(badgeId))
                        .param("severity", "MEDIUM")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "createdDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        MvcResult list = mockMvc.perform(get("/api/v1/employees/{employeeId}/alerts", employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        long alertId = objectMapper.readTree(list.getResponse().getContentAsString())
                .path("data").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/v1/alerts/{id}/close", alertId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    @DisplayName("multiple denied accesses generate CRITICAL alert")
    void multipleDenied() throws Exception {
        Instant base = Instant.parse("2026-07-26T12:00:00Z");
        for (int i = 0; i < 3; i++) {
            createAccessEvent(base.plusSeconds(i * 30L), badgeId, "DENIED");
        }

        // Refresh token: test JWT TTL is short (2s)
        accessToken = login();

        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("type", "MULTIPLE_ACCESS_DENIED")
                        .param("badgeId", String.valueOf(badgeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
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

    private void createAccessEvent(Instant ts, Long badge, String result) throws Exception {
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
                                """.formatted(ts, result, badge, accessPointId)))
                .andExpect(status().isCreated());
    }

    private Long createDepartment() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alert Dept","code":"ALR","description":"Alert dept"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private Long createZone(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/zones")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "building": "Alert Building",
                                  "floor": "1",
                                  "sensitive": false
                                }
                                """.formatted(name)))
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
                                  "name": "Alert Profile %s",
                                  "description": "profile",
                                  "securityLevel": "MEDIUM",
                                  "startTime": "08:00:00",
                                  "endTime": "18:00:00",
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
                                  "employeeNumber": "EMP-ALR-1",
                                  "firstName": "Alert",
                                  "lastName": "User",
                                  "email": "alert.user@flypro.local",
                                  "position": "Guard",
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

    private Long createBadge(String number) throws Exception {
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
                                """.formatted(number, LocalDate.now().minusDays(5), LocalDate.now().plusYears(1))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }
}
