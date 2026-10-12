package com.flypro.analytics.accessevent;

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
class AccessEventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private Long zoneId;
    private Long accessPointId;
    private Long activeBadgeId;
    private Long expiredBadgeId;
    private Long revokedBadgeId;
    private Long suspendedBadgeId;

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

        zoneId = createZone("AE Zone");
        accessPointId = createAccessPoint("AE Door", "AP-AE-01", zoneId);

        Long departmentId = createDepartment();
        Long employeeId = createEmployee(departmentId);

        activeBadgeId = createBadge("BDG-AE-ACTIVE", "ACTIVE");
        expiredBadgeId = createBadge("BDG-AE-EXPIRED", "ACTIVE");
        revokedBadgeId = createBadge("BDG-AE-REVOKED", "ACTIVE");
        suspendedBadgeId = createBadge("BDG-AE-SUSP", "ACTIVE");

        // Force status transitions via API
        mockMvc.perform(patch("/api/v1/badges/{id}/revoke", revokedBadgeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/badges/{id}/suspend", suspendedBadgeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Expire via renew with past date is blocked - use update with past expiration
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/v1/badges/{id}", expiredBadgeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "badgeNumber": "BDG-AE-EXPIRED",
                                  "type": "RFID",
                                  "status": "EXPIRED",
                                  "issueDate": "%s",
                                  "expirationDate": "%s"
                                }
                                """.formatted(
                                LocalDate.now().minusYears(2),
                                LocalDate.now().minusDays(1))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/badges/{id}/assign/{employeeId}", activeBadgeId, employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("create authorized event and search by date/result")
    void createAndSearch() throws Exception {
        Instant ts = Instant.parse("2026-07-26T10:15:30Z");

        MvcResult create = mockMvc.perform(post("/api/v1/access-events")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(ts, activeBadgeId, "AUTHORIZED")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.result").value("AUTHORIZED"))
                .andExpect(jsonPath("$.data.employeeId").isNumber())
                .andExpect(jsonPath("$.data.zone.id").value(zoneId))
                .andReturn();

        long eventId = objectMapper.readTree(create.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/access-events/{id}", eventId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.badgeNumber").value("BDG-AE-ACTIVE"));

        mockMvc.perform(get("/api/v1/access-events")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("result", "AUTHORIZED")
                        .param("badgeNumber", "BDG-AE")
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-07-31T23:59:59Z")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("expired / revoked / suspended badges produce expected results")
    void badgeStatusResults() throws Exception {
        Instant ts = Instant.parse("2026-07-26T11:00:00Z");

        mockMvc.perform(post("/api/v1/access-events")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(ts, expiredBadgeId, "AUTHORIZED")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.result").value("EXPIRED"));

        mockMvc.perform(post("/api/v1/access-events")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(ts, revokedBadgeId, "AUTHORIZED")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.result").value("DENIED"))
                .andExpect(jsonPath("$.data.refusalReason").value("Badge is revoked"));

        mockMvc.perform(post("/api/v1/access-events")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(ts, suspendedBadgeId, "AUTHORIZED")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.result").value("DENIED"))
                .andExpect(jsonPath("$.data.refusalReason").value("Badge is suspended"));
    }

    @Test
    @DisplayName("badge / access point / zone not found and zone mismatch")
    void notFoundAndMismatch() throws Exception {
        Instant ts = Instant.parse("2026-07-26T12:00:00Z");

        mockMvc.perform(post("/api/v1/access-events")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(ts, 999999L, "AUTHORIZED")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BADGE_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/access-events")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventTimestamp": "%s",
                                  "direction": "ENTRY",
                                  "result": "AUTHORIZED",
                                  "source": "API",
                                  "badgeId": %d,
                                  "accessPointId": 999999,
                                  "zoneId": %d
                                }
                                """.formatted(ts, activeBadgeId, zoneId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCESS_POINT_NOT_FOUND"));

        Long otherZone = createZone("Other AE Zone");
        mockMvc.perform(post("/api/v1/access-events")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventTimestamp": "%s",
                                  "direction": "ENTRY",
                                  "result": "AUTHORIZED",
                                  "source": "API",
                                  "badgeId": %d,
                                  "accessPointId": %d,
                                  "zoneId": %d
                                }
                                """.formatted(ts, activeBadgeId, accessPointId, otherZone)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_EVENT"));
    }

    @Test
    @DisplayName("unauthorized without token")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/access-events"))
                .andExpect(status().isUnauthorized());
    }

    private String eventBody(Instant ts, Long badgeId, String result) {
        return """
                {
                  "eventTimestamp": "%s",
                  "direction": "ENTRY",
                  "result": "%s",
                  "source": "API",
                  "rawPayload": "{\\"readerId\\":\\"R-01\\"}",
                  "badgeId": %d,
                  "accessPointId": %d,
                  "zoneId": %d
                }
                """.formatted(ts, result, badgeId, accessPointId, zoneId);
    }

    private Long createZone(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/zones")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "building": "Building North",
                                  "floor": "1",
                                  "sensitive": false
                                }
                                """.formatted(name)))
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

    private Long createDepartment() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"AE Dept","code":"AED","description":"Access event dept"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private Long createEmployee(Long departmentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeNumber": "EMP-AE-1",
                                  "firstName": "Sara",
                                  "lastName": "Benali",
                                  "email": "sara.ae@flypro.local",
                                  "position": "Analyst",
                                  "status": "ACTIVE",
                                  "departmentId": %d
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private Long createBadge(String number, String status) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/badges")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "badgeNumber": "%s",
                                  "type": "RFID",
                                  "status": "%s",
                                  "issueDate": "%s",
                                  "expirationDate": "%s"
                                }
                                """.formatted(
                                number, status,
                                LocalDate.now().minusDays(5),
                                LocalDate.now().plusYears(1))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }
}
