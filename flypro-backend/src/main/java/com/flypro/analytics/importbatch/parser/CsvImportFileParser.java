package com.flypro.analytics.importbatch.parser;

import com.flypro.analytics.common.enums.ImportFileType;
import com.flypro.analytics.importbatch.exception.InvalidFileFormatException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV access-event file parser (OpenCSV).
 */
@Component
public class CsvImportFileParser implements ImportFileParser {

    @Override
    public ImportFileType supports() {
        return ImportFileType.CSV;
    }

    @Override
    public List<ImportRawRecord> parse(byte[] content, String fileName) {
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()) {
                throw new InvalidFileFormatException("CSV file is empty: " + fileName);
            }

            String[] headers = rows.getFirst();
            List<ImportRawRecord> records = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (isBlankRow(row)) {
                    continue;
                }
                Map<String, String> raw = new LinkedHashMap<>();
                for (int c = 0; c < headers.length; c++) {
                    String header = headers[c] == null ? "" : headers[c].trim();
                    String value = c < row.length ? row[c] : null;
                    raw.put(header, value);
                }
                records.add(new ImportRawRecord(i + 1, ImportColumnNormalizer.mapRow(raw)));
            }
            return records;
        } catch (IOException | CsvException ex) {
            throw new InvalidFileFormatException("Unable to parse CSV file: " + ex.getMessage());
        }
    }

    private boolean isBlankRow(String[] row) {
        if (row == null || row.length == 0) {
            return true;
        }
        for (String cell : row) {
            if (cell != null && !cell.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
