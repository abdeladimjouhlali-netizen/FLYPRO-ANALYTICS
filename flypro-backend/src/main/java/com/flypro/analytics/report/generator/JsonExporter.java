package com.flypro.analytics.report.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flypro.analytics.report.dto.ReportResponseDTO;
import com.flypro.analytics.report.enums.ExportFormat;
import com.flypro.analytics.report.exception.ExportException;
import com.flypro.analytics.report.model.ReportDocument;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * JSON exporter.
 */
@Component
public class JsonExporter implements ReportGenerator {

    private final ObjectMapper objectMapper;

    public JsonExporter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public ExportFormat format() {
        return ExportFormat.JSON;
    }

    @Override
    public byte[] generate(ReportDocument document) {
        try {
            ReportResponseDTO dto = new ReportResponseDTO(
                    document.getReportType(),
                    document.getReportKind(),
                    document.getTitle(),
                    document.getGeneratedAt(),
                    document.getSummary(),
                    document.getColumns(),
                    document.getRows(),
                    document.getRows().size()
            );
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(dto)
                    .getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new ExportException("Failed to generate JSON report", ex);
        }
    }
}
