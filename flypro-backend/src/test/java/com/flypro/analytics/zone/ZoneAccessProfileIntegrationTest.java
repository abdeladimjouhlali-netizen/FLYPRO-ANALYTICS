package com.flypro.analytics.zone;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ZoneAccessProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

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
    }

    @Test
    @DisplayName("zone and access profile lifecycle with assign/remove")
    void lifecycle() throws Exception {
        long zone1 = createZone("Server Room A", true);
        long zone2 = createZone("Lobby", false);

        mockMvc.perform(get("/api/v1/zones")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("sensitive", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        MvcResult profileCreate = mockMvc.perform(post("/api/v1/access-profiles")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Standard Office",
                                  "description": "Weekday access",
                                  "securityLevel": "MEDIUM",
                                  "startTime": "08:00:00",
                                  "endTime": "18:00:00",
                                  "zoneIds": [%d]
                                }
                                """.formatted(zone1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.zones.length()").value(1))
                .andReturn();

        long profileId = objectMapper.readTree(profileCreate.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/access-profiles/{id}/zones/{zoneId}", profileId, zone2)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.zones.length()").value(2));

        mockMvc.perform(get("/api/v1/access-profiles")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("zoneId", String.valueOf(zone1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(delete("/api/v1/access-profiles/{id}/zones/{zoneId}", profileId, zone1)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.zones.length()").value(1));

        mockMvc.perform(put("/api/v1/zones/{id}", zone2)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Main Lobby",
                                  "building": "Building North",
                                  "floor": "0",
                                  "sensitive": false,
                                  "description": "Updated lobby"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Main Lobby"));

        mockMvc.perform(delete("/api/v1/access-profiles/{id}", profileId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/zones/{id}", zone1)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/zones/{id}", zone2)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("duplicate names and invalid working hours")
    void validationRules() throws Exception {
        createZone("Unique Zone", false);

        mockMvc.perform(post("/api/v1/zones")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unique Zone",
                                  "building": "B2",
                                  "floor": "1",
                                  "sensitive": false
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ZONE"));

        long zoneId = createZone("Hours Zone", false);

        mockMvc.perform(post("/api/v1/access-profiles")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bad Hours",
                                  "securityLevel": "LOW",
                                  "startTime": "18:00:00",
                                  "endTime": "08:00:00",
                                  "zoneIds": [%d]
                                }
                                """.formatted(zoneId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WORKING_HOURS"));
    }

    @Test
    @DisplayName("unauthorized without token")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/zones")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/access-profiles")).andExpect(status().isUnauthorized());
    }

    private long createZone(String name, boolean sensitive) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/zones")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "building": "Building North",
                                  "floor": "1",
                                  "sensitive": %s,
                                  "description": "%s zone"
                                }
                                """.formatted(name, sensitive, name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }
}
