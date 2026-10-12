package com.flypro.analytics.importbatch.dto;

import com.flypro.analytics.common.enums.ImportStatus;

import java.time.Instant;

/**
 * Search / filter criteria for import history.
 */
public record ImportSearchCriteria(
        ImportStatus status,
        String fileName,
        Instant importDateFrom,
        Instant importDateTo,
        String createdBy,
        String keyword
) {
}
