package com.flypro.analytics.importbatch.dto;

import com.flypro.analytics.common.enums.ImportFileType;
import com.flypro.analytics.common.enums.ImportStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Aggregated import / preview report.
 */
@Schema(description = "Import summary / validation report")
public record ImportSummaryDTO(

        @Schema(example = "1")
        Long batchId,

        @Schema(example = "access-events.csv")
        String fileName,

        @Schema(example = "CSV")
        ImportFileType fileType,

        @Schema(example = "COMPLETED")
        ImportStatus status,

        @Schema(example = "100", description = "Total records read from file")
        int totalRecords,

        @Schema(example = "95", description = "Successfully imported records")
        int importedRecords,

        @Schema(example = "3", description = "Rejected records")
        int rejectedRecords,

        @Schema(example = "2", description = "Duplicate records ignored")
        int duplicateRecords,

        @Schema(description = "Validation errors (rejected rows)")
        List<DataQualityIssueDTO> validationErrors,

        @Schema(description = "Warnings (e.g. duplicates, missing employee assignment)")
        List<DataQualityIssueDTO> warnings,

        @Schema(example = "1250", description = "Processing time in milliseconds")
        long processingTimeMs
) {
}
