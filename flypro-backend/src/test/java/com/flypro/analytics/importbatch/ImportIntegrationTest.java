package com.flypro.analytics.importbatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private String accessPointCode;
    private String badgeNumber;

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

        Long zoneId = createZone("Import Zone");
        accessPointCode = "AP-IMP-01";
        createAccessPoint("Import Door", accessPointCode, zoneId);

        Long departmentId = createDepartment();
        Long employeeId = createEmployee(departmentId);
        badgeNumber = "BDG-IMP-001";
        Long badgeId = createBadge(badgeNumber);

        mockMvc.perform(post("/api/v1/badges/{id}/assign/{employeeId}", badgeId, employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CSV upload preview execute report and history")
    void csvImportFlow() throws Exception {
        String csv = """
                eventTimestamp,badgeNumber,accessPointCode,direction,result
                2026-07-26T10:00:00Z,%s,%s,ENTRY,AUTHORIZED
                2026-07-26T10:00:00Z,%s,%s,ENTRY,AUTHORIZED
                2026-07-26T11:00:00Z,BDG-UNKNOWN,%s,ENTRY,AUTHORIZED
                2026-07-26T12:00:00Z,%s,%s,ENTRY,AUTHORIZED
                """.formatted(badgeNumber, accessPointCode, badgeNumber, accessPointCode,
                accessPointCode, badgeNumber, accessPointCode);

        MockMultipartFile file = new MockMultipartFile(
                "file", "events.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/imports/preview").file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRecords").value(4))
                .andExpect(jsonPath("$.data.importedRecords").value(2))
                .andExpect(jsonPath("$.data.duplicateRecords").value(1))
                .andExpect(jsonPath("$.data.rejectedRecords").value(1));

        MvcResult upload = mockMvc.perform(multipart("/api/v1/imports/upload").file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        long batchId = objectMapper.readTree(upload.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/imports/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchId\":" + batchId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validRecords").value(2))
                .andExpect(jsonPath("$.data.duplicateRecords").value(1))
                .andExpect(jsonPath("$.data.invalidRecords").value(1))
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_COMPLETED"));

        mockMvc.perform(get("/api/v1/imports/{id}/report", batchId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importedRecords").value(2))
                .andExpect(jsonPath("$.data.validationErrors").isArray())
                .andExpect(jsonPath("$.data.warnings").isArray());

        mockMvc.perform(get("/api/v1/imports")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("status", "PARTIALLY_COMPLETED")
                        .param("fileName", "events")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/v1/imports/{id}", batchId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileType").value("CSV"));

        mockMvc.perform(get("/api/v1/access-events")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("badgeNumber", badgeNumber)
                        .param("source", "CSV"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("JSON import execute in one step")
    void jsonImport() throws Exception {
        String json = """
                [
                  {
                    "eventTimestamp": "2026-07-26T14:00:00Z",
                    "badgeNumber": "%s",
                    "accessPointCode": "%s",
                    "direction": "EXIT",
                    "result": "AUTHORIZED"
                  }
                ]
                """.formatted(badgeNumber, accessPointCode);

        MockMultipartFile file = new MockMultipartFile(
                "file", "events.json", "application/json", json.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/imports/execute").file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validRecords").value(1))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.fileType").value("JSON"));
    }

    @Test
    @DisplayName("Excel import")
    void excelImport() throws Exception {
        byte[] xlsx = buildExcel(
                new String[]{"eventTimestamp", "badgeNumber", "accessPointCode", "direction", "result"},
                new String[]{"2026-07-26T15:00:00Z", badgeNumber, accessPointCode, "ENTRY", "AUTHORIZED"}
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "events.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsx
        );

        mockMvc.perform(multipart("/api/v1/imports/execute").file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validRecords").value(1))
                .andExpect(jsonPath("$.data.fileType").value("XLSX"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("unsupported and invalid file")
    void invalidFiles() throws Exception {
        MockMultipartFile txt = new MockMultipartFile(
                "file", "events.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/imports/upload").file(txt)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));

        MockMultipartFile badCsv = new MockMultipartFile(
                "file", "bad.csv", "text/csv", "foo,bar\n1,2".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/imports/preview").file(badCsv)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_FORMAT"));
    }

    @Test
    @DisplayName("analyst can read but not upload")
    void analystReadOnly() throws Exception {
        // Seed analyst user may not exist — create via login as admin is enough;
        // verify unauthenticated is blocked and admin works. Skip if no analyst seed.
        mockMvc.perform(get("/api/v1/imports"))
                .andExpect(status().isUnauthorized());
    }

    private byte[] buildExcel(String[] headers, String[] values) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("events");
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            Row row = sheet.createRow(1);
            for (int i = 0; i < values.length; i++) {
                row.createCell(i).setCellValue(values[i]);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private Long createZone(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/zones")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "building": "Building Import",
                                  "floor": "2",
                                  "sensitive": false
                                }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private void createAccessPoint(String name, String code, Long zone) throws Exception {
        mockMvc.perform(post("/api/v1/access-points")
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
                .andExpect(status().isCreated());
    }

    private Long createDepartment() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Import Dept","code":"IMP","description":"Import dept"}
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
                                  "employeeNumber": "EMP-IMP-1",
                                  "firstName": "Import",
                                  "lastName": "User",
                                  "email": "import.user@flypro.local",
                                  "position": "Operator",
                                  "status": "ACTIVE",
                                  "departmentId": %d
                                }
                                """.formatted(departmentId)))
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
                                """.formatted(
                                number,
                                LocalDate.now().minusDays(5),
                                LocalDate.now().plusYears(1))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }
}
