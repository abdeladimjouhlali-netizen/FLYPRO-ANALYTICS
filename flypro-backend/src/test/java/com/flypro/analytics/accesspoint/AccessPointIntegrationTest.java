package com.flypro.analytics.accesspoint;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccessPointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private Long zoneId;

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

        MvcResult zone = mockMvc.perform(post("/api/v1/zones")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "AP Integration Zone",
                                  "building": "Building North",
                                  "floor": "1",
                                  "sensitive": false,
                                  "description": "Integration zone"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        zoneId = objectMapper.readTree(zone.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    @DisplayName("full access point lifecycle")
    void lifecycle() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/access-points")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Main Door", "AP-INT-01")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.zone.id").value(zoneId))
                .andReturn();

        long id = objectMapper.readTree(create.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/access-points")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("zoneId", String.valueOf(zoneId))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/v1/zones/{zoneId}/access-points", zoneId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(patch("/api/v1/access-points/{id}/deactivate", id)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(patch("/api/v1/access-points/{id}/maintenance", id)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MAINTENANCE"));

        mockMvc.perform(patch("/api/v1/access-points/{id}/activate", id)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(put("/api/v1/access-points/{id}", id)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Main Door Updated",
                                  "code": "AP-INT-01",
                                  "type": "TURNSTILE",
                                  "description": "Updated",
                                  "status": "ACTIVE",
                                  "ipAddress": "10.0.0.5",
                                  "location": "South",
                                  "zoneId": %d
                                }
                                """.formatted(zoneId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("TURNSTILE"));

        mockMvc.perform(delete("/api/v1/access-points/{id}", id)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("duplicate code and name are rejected")
    void duplicates() throws Exception {
        mockMvc.perform(post("/api/v1/access-points")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Door One", "AP-DUP")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/access-points")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Door Two", "AP-DUP")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ACCESS_POINT_CODE"));

        mockMvc.perform(post("/api/v1/access-points")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Door One", "AP-OTHER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ACCESS_POINT_NAME"));
    }

    @Test
    @DisplayName("zone not found and invalid IP")
    void validationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/access-points")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bad Zone AP",
                                  "code": "AP-BAD-Z",
                                  "type": "DOOR",
                                  "status": "ACTIVE",
                                  "zoneId": 999999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ZONE_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/access-points")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bad IP AP",
                                  "code": "AP-BAD-IP",
                                  "type": "DOOR",
                                  "status": "ACTIVE",
                                  "ipAddress": "not-an-ip",
                                  "zoneId": %d
                                }
                                """.formatted(zoneId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IP_ADDRESS"));
    }

    @Test
    @DisplayName("unauthorized without token")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/access-points"))
                .andExpect(status().isUnauthorized());
    }

    private String body(String name, String code) {
        return """
                {
                  "name": "%s",
                  "code": "%s",
                  "type": "DOOR",
                  "description": "Test AP",
                  "status": "ACTIVE",
                  "ipAddress": "192.168.1.20",
                  "location": "Lobby",
                  "zoneId": %d
                }
                """.formatted(name, code, zoneId);
    }
}
