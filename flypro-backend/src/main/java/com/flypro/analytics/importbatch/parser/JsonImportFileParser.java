package com.flypro.analytics.importbatch.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flypro.analytics.common.enums.ImportFileType;
import com.flypro.analytics.importbatch.exception.InvalidFileFormatException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON access-event file parser (Jackson).
 * Accepts a root array or an object with a {@code records} / {@code events} / {@code data} array.
 */
@Component
public class JsonImportFileParser implements ImportFileParser {

    private final ObjectMapper objectMapper;

    public JsonImportFileParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ImportFileType supports() {
        return ImportFileType.JSON;
    }

    @Override
    public List<ImportRawRecord> parse(byte[] content, String fileName) {
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode array = resolveArray(root, fileName);

            List<ImportRawRecord> records = new ArrayList<>();
            int index = 0;
            for (JsonNode node : array) {
                index++;
                if (node == null || node.isNull()) {
                    continue;
                }
                if (!node.isObject()) {
                    throw new InvalidFileFormatException(
                            "JSON record at index " + index + " must be an object");
                }
                Map<String, String> raw = new LinkedHashMap<>();
                Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    JsonNode value = entry.getValue();
                    raw.put(entry.getKey(), value == null || value.isNull() ? null : value.asText());
                }
                records.add(new ImportRawRecord(index, ImportColumnNormalizer.mapRow(raw)));
            }
            return records;
        } catch (IOException ex) {
            throw new InvalidFileFormatException("Unable to parse JSON file: " + ex.getMessage());
        }
    }

    private JsonNode resolveArray(JsonNode root, String fileName) {
        if (root == null || root.isNull()) {
            throw new InvalidFileFormatException("JSON file is empty: " + fileName);
        }
        if (root.isArray()) {
            return root;
        }
        if (root.isObject()) {
            for (String key : List.of("records", "events", "data", "items")) {
                JsonNode child = root.get(key);
                if (child != null && child.isArray()) {
                    return child;
                }
            }
        }
        throw new InvalidFileFormatException(
                "JSON root must be an array or an object containing a records/events/data array");
    }
}
