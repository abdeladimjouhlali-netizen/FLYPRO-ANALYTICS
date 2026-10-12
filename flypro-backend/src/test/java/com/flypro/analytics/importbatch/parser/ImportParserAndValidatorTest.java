package com.flypro.analytics.importbatch.parser;

import com.flypro.analytics.common.enums.ImportFileType;
import com.flypro.analytics.importbatch.exception.InvalidFileFormatException;
import com.flypro.analytics.importbatch.exception.UnsupportedFileTypeException;
import com.flypro.analytics.importbatch.validator.ImportValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportParserAndValidatorTest {

    private final CsvImportFileParser csvParser = new CsvImportFileParser();
    private final JsonImportFileParser jsonParser = new JsonImportFileParser(new com.fasterxml.jackson.databind.ObjectMapper());
    private final ImportValidator validator = new ImportValidator();

    @Test
    @DisplayName("CSV parser maps headers and skips blank rows")
    void csvParse() {
        String csv = """
                event_timestamp,badge_number,access_point_code,direction,result
                2026-07-26T10:00:00Z,BDG-1,AP-1,ENTRY,AUTHORIZED
                
                """;
        List<ImportRawRecord> records = csvParser.parse(csv.getBytes(StandardCharsets.UTF_8), "t.csv");
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().get(ImportColumnNormalizer.BADGE_NUMBER)).isEqualTo("BDG-1");
        assertThat(csvParser.supports()).isEqualTo(ImportFileType.CSV);
    }

    @Test
    @DisplayName("JSON parser accepts records wrapper")
    void jsonParse() {
        String json = """
                {"records":[{"eventTimestamp":"2026-07-26T10:00:00Z","badgeNumber":"BDG-1",
                "accessPointCode":"AP-1","direction":"ENTRY","result":"AUTHORIZED"}]}
                """;
        List<ImportRawRecord> records = jsonParser.parse(json.getBytes(StandardCharsets.UTF_8), "t.json");
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().lineNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("structure validation requires mandatory columns")
    void structureValidation() {
        assertThatThrownBy(() -> validator.validateStructure(List.of(
                new ImportRawRecord(1, java.util.Map.of("badgeNumber", "X"))
        ))).isInstanceOf(InvalidFileFormatException.class);

        List<ImportRawRecord> ok = List.of(new ImportRawRecord(1, java.util.Map.of(
                "eventTimestamp", "2026-07-26T10:00:00Z",
                "badgeNumber", "BDG",
                "accessPointCode", "AP",
                "direction", "ENTRY",
                "result", "AUTHORIZED"
        )));
        validator.validateStructure(ok);
        assertThat(validator.validateRow(ok.getFirst())).isEmpty();
    }

    @Test
    @DisplayName("file type detection rejects unknown extensions")
    void detectType() {
        ImportFileParserRegistry registry = new ImportFileParserRegistry(List.of(csvParser, jsonParser));
        assertThat(registry.detect("a.csv", "text/csv")).isEqualTo(ImportFileType.CSV);
        assertThat(registry.detect("a.json", "application/json")).isEqualTo(ImportFileType.JSON);
        assertThatThrownBy(() -> registry.detect("a.txt", "text/plain"))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }
}
