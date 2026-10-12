package com.flypro.analytics.importbatch.validator;

import com.flypro.analytics.common.enums.AccessDirection;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.importbatch.exception.DataValidationException;
import com.flypro.analytics.importbatch.exception.InvalidFileFormatException;
import com.flypro.analytics.importbatch.parser.ImportColumnNormalizer;
import com.flypro.analytics.importbatch.parser.ImportRawRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * Structural and row-level validation for access-event import files.
 */
@Component
public class ImportValidator {

    public void validateFilePresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DataValidationException("Import file is required and must not be empty");
        }
        if (!StringUtils.hasText(file.getOriginalFilename())) {
            throw new DataValidationException("Import file name is required");
        }
    }

    /**
     * Ensures the parsed dataset exposes all mandatory columns at least once.
     */
    public void validateStructure(List<ImportRawRecord> records) {
        if (records == null || records.isEmpty()) {
            throw new InvalidFileFormatException("Import file contains no data rows");
        }

        EnumSet<MissingColumn> missing = EnumSet.allOf(MissingColumn.class);
        for (ImportRawRecord record : records) {
            if (record.get(ImportColumnNormalizer.EVENT_TIMESTAMP) != null) {
                missing.remove(MissingColumn.EVENT_TIMESTAMP);
            }
            if (record.get(ImportColumnNormalizer.BADGE_NUMBER) != null) {
                missing.remove(MissingColumn.BADGE_NUMBER);
            }
            if (record.get(ImportColumnNormalizer.ACCESS_POINT_CODE) != null) {
                missing.remove(MissingColumn.ACCESS_POINT_CODE);
            }
            if (record.get(ImportColumnNormalizer.DIRECTION) != null) {
                missing.remove(MissingColumn.DIRECTION);
            }
            if (record.get(ImportColumnNormalizer.ACCESS_RESULT) != null) {
                missing.remove(MissingColumn.ACCESS_RESULT);
            }
            if (missing.isEmpty()) {
                break;
            }
        }

        // Also accept headers that mapped but all values null — check field keys
        for (ImportRawRecord record : records) {
            missing.removeIf(col -> record.fields().containsKey(col.canonical));
            if (missing.isEmpty()) {
                break;
            }
        }

        if (!missing.isEmpty()) {
            List<String> names = missing.stream().map(m -> m.canonical).toList();
            throw new InvalidFileFormatException(
                    "Missing mandatory columns: " + String.join(", ", names)
            );
        }
    }

    /**
     * Validates a single raw row and returns field-level issues (does not throw for row errors).
     */
    public List<RowIssue> validateRow(ImportRawRecord record) {
        List<RowIssue> issues = new ArrayList<>();

        require(record, ImportColumnNormalizer.EVENT_TIMESTAMP, issues);
        require(record, ImportColumnNormalizer.BADGE_NUMBER, issues);
        require(record, ImportColumnNormalizer.ACCESS_POINT_CODE, issues);
        require(record, ImportColumnNormalizer.DIRECTION, issues);
        require(record, ImportColumnNormalizer.ACCESS_RESULT, issues);

        String timestamp = record.get(ImportColumnNormalizer.EVENT_TIMESTAMP);
        if (timestamp != null) {
            try {
                Instant.parse(timestamp);
            } catch (DateTimeParseException ex) {
                issues.add(new RowIssue(
                        record.lineNumber(),
                        ImportColumnNormalizer.EVENT_TIMESTAMP,
                        "Invalid timestamp format (expected ISO-8601 Instant): " + timestamp,
                        AlertSeverity.HIGH
                ));
            }
        }

        String direction = record.get(ImportColumnNormalizer.DIRECTION);
        if (direction != null && parseDirection(direction) == null) {
            issues.add(new RowIssue(
                    record.lineNumber(),
                    ImportColumnNormalizer.DIRECTION,
                    "Invalid direction: " + direction + " (allowed: ENTRY, EXIT, NEUTRAL)",
                    AlertSeverity.HIGH
            ));
        }

        String result = record.get(ImportColumnNormalizer.ACCESS_RESULT);
        if (result != null && parseResult(result) == null) {
            issues.add(new RowIssue(
                    record.lineNumber(),
                    ImportColumnNormalizer.ACCESS_RESULT,
                    "Invalid access result: " + result,
                    AlertSeverity.HIGH
            ));
        }

        return issues;
    }

    public Instant parseTimestamp(String value) {
        return Instant.parse(value);
    }

    public AccessDirection parseDirection(String value) {
        try {
            return AccessDirection.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return null;
        }
    }

    public AccessResult parseResult(String value) {
        try {
            return AccessResult.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return null;
        }
    }

    private void require(ImportRawRecord record, String field, List<RowIssue> issues) {
        if (!StringUtils.hasText(record.get(field))) {
            issues.add(new RowIssue(
                    record.lineNumber(),
                    field,
                    "Mandatory field is missing: " + field,
                    AlertSeverity.HIGH
            ));
        }
    }

    public record RowIssue(int lineNumber, String fieldName, String message, AlertSeverity severity) {
    }

    private enum MissingColumn {
        EVENT_TIMESTAMP(ImportColumnNormalizer.EVENT_TIMESTAMP),
        BADGE_NUMBER(ImportColumnNormalizer.BADGE_NUMBER),
        ACCESS_POINT_CODE(ImportColumnNormalizer.ACCESS_POINT_CODE),
        DIRECTION(ImportColumnNormalizer.DIRECTION),
        ACCESS_RESULT(ImportColumnNormalizer.ACCESS_RESULT);

        private final String canonical;

        MissingColumn(String canonical) {
            this.canonical = canonical;
        }
    }
}
