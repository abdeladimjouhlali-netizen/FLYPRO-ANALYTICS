package com.flypro.analytics.importbatch.parser;

import com.flypro.analytics.common.enums.ImportFileType;

import java.util.List;

/**
 * Strategy for parsing an uploaded import file into raw records.
 */
public interface ImportFileParser {

    ImportFileType supports();

    /**
     * Parse file bytes into raw records (line numbers are 1-based data rows).
     */
    List<ImportRawRecord> parse(byte[] content, String fileName);
}
