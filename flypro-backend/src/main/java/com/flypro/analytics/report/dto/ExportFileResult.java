package com.flypro.analytics.report.dto;

/**
 * Binary export result.
 */
public record ExportFileResult(
        String fileName,
        String contentType,
        byte[] content
) {
}
