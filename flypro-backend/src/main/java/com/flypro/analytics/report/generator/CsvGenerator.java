package com.flypro.analytics.report.generator;

import com.flypro.analytics.report.enums.ExportFormat;
import com.flypro.analytics.report.exception.ExportException;
import com.flypro.analytics.report.model.ReportDocument;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CSV exporter (UTF-8 with BOM for Excel compatibility).
 */
@Component
public class CsvGenerator implements ReportGenerator {

    @Override
    public ExportFormat format() {
        return ExportFormat.CSV;
    }

    @Override
    public byte[] generate(ReportDocument document) {
        try (StringWriter sw = new StringWriter();
             CSVWriter writer = new CSVWriter(sw)) {
            if (!document.getColumns().isEmpty()) {
                writer.writeNext(document.getColumns().toArray(String[]::new));
                for (Map<String, Object> row : document.getRows()) {
                    List<String> values = new ArrayList<>(document.getColumns().size());
                    for (String col : document.getColumns()) {
                        Object v = row.get(col);
                        values.add(v == null ? "" : String.valueOf(v));
                    }
                    writer.writeNext(values.toArray(String[]::new));
                }
            }
            // BOM + content
            byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
            byte[] body = sw.toString().getBytes(StandardCharsets.UTF_8);
            byte[] result = new byte[bom.length + body.length];
            System.arraycopy(bom, 0, result, 0, bom.length);
            System.arraycopy(body, 0, result, bom.length, body.length);
            return result;
        } catch (Exception ex) {
            throw new ExportException("Failed to generate CSV report", ex);
        }
    }
}
