package com.flypro.analytics.importbatch.parser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One raw row extracted from an import file.
 */
public record ImportRawRecord(
        int lineNumber,
        Map<String, String> fields
) {
    public ImportRawRecord {
        fields = fields == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public String get(String canonicalField) {
        return fields.get(canonicalField);
    }
}
