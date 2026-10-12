package com.flypro.analytics.importbatch.parser;

import com.flypro.analytics.common.enums.ImportFileType;
import com.flypro.analytics.importbatch.exception.InvalidFileFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel (.xlsx) access-event file parser (Apache POI).
 */
@Component
public class ExcelImportFileParser implements ImportFileParser {

    private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;
    private final DataFormatter dataFormatter = new DataFormatter();

    @Override
    public ImportFileType supports() {
        return ImportFileType.XLSX;
    }

    @Override
    public List<ImportRawRecord> parse(byte[] content, String fileName) {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new InvalidFileFormatException("Excel file has no data sheet: " + fileName);
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new InvalidFileFormatException("Excel file is missing a header row: " + fileName);
            }

            List<String> headers = readHeaders(headerRow);
            List<ImportRawRecord> records = new ArrayList<>();

            for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row, headers.size())) {
                    continue;
                }
                Map<String, String> raw = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    raw.put(headers.get(c), cellAsString(row.getCell(c)));
                }
                records.add(new ImportRawRecord(r + 1, ImportColumnNormalizer.mapRow(raw)));
            }
            return records;
        } catch (IOException ex) {
            throw new InvalidFileFormatException("Unable to parse Excel file: " + ex.getMessage());
        }
    }

    private List<String> readHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        short last = headerRow.getLastCellNum();
        for (int c = 0; c < last; c++) {
            Cell cell = headerRow.getCell(c);
            String value = cell == null ? "" : dataFormatter.formatCellValue(cell).trim();
            headers.add(value);
        }
        return headers;
    }

    private boolean isBlankRow(Row row, int columnCount) {
        for (int c = 0; c < columnCount; c++) {
            String value = cellAsString(row.getCell(c));
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String cellAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return ISO_INSTANT.format(cell.getDateCellValue().toInstant().atOffset(ZoneOffset.UTC));
        }
        String formatted = dataFormatter.formatCellValue(cell);
        return formatted == null || formatted.isBlank() ? null : formatted.trim();
    }
}
