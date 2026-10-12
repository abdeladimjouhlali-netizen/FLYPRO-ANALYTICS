package com.flypro.analytics.importbatch.parser;

import com.flypro.analytics.common.enums.ImportFileType;
import com.flypro.analytics.importbatch.exception.UnsupportedFileTypeException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Detects file type and dispatches to the matching {@link ImportFileParser}.
 */
@Component
public class ImportFileParserRegistry {

    private final Map<ImportFileType, ImportFileParser> parsers;

    public ImportFileParserRegistry(List<ImportFileParser> parserList) {
        this.parsers = new EnumMap<>(ImportFileType.class);
        for (ImportFileParser parser : parserList) {
            this.parsers.put(parser.supports(), parser);
        }
    }

    public ImportFileType detect(MultipartFile file) {
        return detect(file.getOriginalFilename(), file.getContentType());
    }

    public ImportFileType detect(String fileName, String contentType) {
        String name = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        String type = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);

        if (name.endsWith(".csv") || type.contains("text/csv") || type.contains("application/csv")) {
            return ImportFileType.CSV;
        }
        if (name.endsWith(".xlsx")
                || type.contains("spreadsheetml")
                || type.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return ImportFileType.XLSX;
        }
        if (name.endsWith(".json") || type.contains("application/json") || type.contains("text/json")) {
            return ImportFileType.JSON;
        }

        String display = StringUtils.hasText(fileName) ? fileName : "unknown";
        throw new UnsupportedFileTypeException(display);
    }

    public List<ImportRawRecord> parse(ImportFileType fileType, byte[] content, String fileName) {
        ImportFileParser parser = parsers.get(fileType);
        if (parser == null) {
            throw new UnsupportedFileTypeException(fileName);
        }
        return parser.parse(content, fileName);
    }
}
