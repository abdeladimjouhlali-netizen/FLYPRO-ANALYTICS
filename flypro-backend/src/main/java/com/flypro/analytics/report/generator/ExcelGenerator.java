package com.flypro.analytics.report.generator;

import com.flypro.analytics.report.enums.ExportFormat;
import com.flypro.analytics.report.exception.ExportException;
import com.flypro.analytics.report.model.ReportDocument;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Excel (.xlsx) exporter based on Apache POI.
 */
@Component
public class ExcelGenerator implements ReportGenerator {

    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public ExportFormat format() {
        return ExportFormat.EXCEL;
    }

    @Override
    public byte[] generate(ReportDocument document) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet(sheetName(document.getTitle()));

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(document.getTitle());
            titleCell.setCellStyle(titleStyle);

            Row metaRow = sheet.createRow(rowIdx++);
            metaRow.createCell(0).setCellValue(
                    "Generated: " + TS.format(document.getGeneratedAt().atOffset(ZoneOffset.UTC))
                            + " | Kind: " + document.getReportKind() + " | Type: " + document.getReportType());

            rowIdx++;

            if (!document.getSummary().isEmpty()) {
                for (Map.Entry<String, Object> e : document.getSummary().entrySet()) {
                    Row summaryRow = sheet.createRow(rowIdx++);
                    summaryRow.createCell(0).setCellValue(e.getKey());
                    summaryRow.createCell(1).setCellValue(stringify(e.getValue()));
                }
                rowIdx++;
            }

            List<String> columns = document.getColumns();
            if (!columns.isEmpty()) {
                Row headerRow = sheet.createRow(rowIdx++);
                for (int c = 0; c < columns.size(); c++) {
                    Cell cell = headerRow.createCell(c);
                    cell.setCellValue(columns.get(c));
                    cell.setCellStyle(headerStyle);
                }
                for (Map<String, Object> dataRow : document.getRows()) {
                    Row row = sheet.createRow(rowIdx++);
                    for (int c = 0; c < columns.size(); c++) {
                        row.createCell(c).setCellValue(stringify(dataRow.get(columns.get(c))));
                    }
                }
                for (int c = 0; c < columns.size(); c++) {
                    sheet.autoSizeColumn(c);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new ExportException("Failed to generate Excel report", ex);
        }
    }

    private static String sheetName(String title) {
        String cleaned = title.replaceAll("[\\[\\]:*?/\\\\]", " ").trim();
        return cleaned.isEmpty() ? "Report" : cleaned.substring(0, Math.min(31, cleaned.length()));
    }

    private static String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
