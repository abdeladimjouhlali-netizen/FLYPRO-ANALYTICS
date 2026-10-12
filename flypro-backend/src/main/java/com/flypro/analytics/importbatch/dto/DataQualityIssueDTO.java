package com.flypro.analytics.importbatch.dto;

import com.flypro.analytics.common.enums.AlertSeverity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Single data-quality finding for an import batch.
 */
@Schema(description = "Data quality issue")
public record DataQualityIssueDTO(

        @Schema(example = "1")
        Long id,

        @Schema(example = "12")
        Integer lineNumber,

        @Schema(example = "badgeNumber")
        String fieldName,

        @Schema(example = "Badge number not found: BDG-999")
        String message,

        @Schema(example = "HIGH")
        AlertSeverity severity
) {
}
