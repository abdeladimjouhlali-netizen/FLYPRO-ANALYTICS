package com.flypro.analytics.report.generator;

import com.flypro.analytics.report.enums.ExportFormat;
import com.flypro.analytics.report.exception.ExportException;
import com.flypro.analytics.report.model.ReportDocument;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * PDF exporter based on OpenPDF.
 */
@Component
public class PdfGenerator implements ReportGenerator {

    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public ExportFormat format() {
        return ExportFormat.PDF;
    }

    @Override
    public byte[] generate(ReportDocument document) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document pdf = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(pdf, out);
            pdf.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            pdf.add(new Paragraph(document.getTitle(), titleFont));
            pdf.add(new Paragraph(
                    "Generated: " + TS.format(document.getGeneratedAt().atOffset(ZoneOffset.UTC))
                            + " | Kind: " + document.getReportKind()
                            + " | Type: " + document.getReportType(),
                    metaFont));
            pdf.add(new Paragraph(" "));

            if (!document.getSummary().isEmpty()) {
                pdf.add(new Paragraph("Summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                for (Map.Entry<String, Object> e : document.getSummary().entrySet()) {
                    pdf.add(new Paragraph(e.getKey() + ": " + stringify(e.getValue()), metaFont));
                }
                pdf.add(new Paragraph(" "));
            }

            if (!document.getColumns().isEmpty()) {
                PdfPTable table = new PdfPTable(document.getColumns().size());
                table.setWidthPercentage(100);
                for (String col : document.getColumns()) {
                    PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
                    cell.setBackgroundColor(new Color(33, 64, 95));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(4);
                    table.addCell(cell);
                }
                for (Map<String, Object> row : document.getRows()) {
                    for (String col : document.getColumns()) {
                        PdfPCell cell = new PdfPCell(new Phrase(stringify(row.get(col)), cellFont));
                        cell.setPadding(3);
                        table.addCell(cell);
                    }
                }
                pdf.add(table);
            }

            pdf.close();
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException ex) {
            throw new ExportException("Failed to generate PDF report", ex);
        }
    }

    private static String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
