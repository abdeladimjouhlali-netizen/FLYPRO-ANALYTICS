package com.flypro.analytics.importbatch.dto;

import com.flypro.analytics.common.enums.ImportFileType;
import com.flypro.analytics.common.enums.ImportStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Import batch details exposed to API clients.
 */
@Schema(description = "Import batch response")
public record ImportResponseDTO(

        @Schema(example = "1")
        Long id,

        @Schema(example = "access-events.csv")
        String fileName,

        @Schema(example = "CSV")
        ImportFileType fileType,

        @Schema(example = "COMPLETED")
        ImportStatus status,

        @Schema(example = "100")
        int totalRecords,

        @Schema(example = "95")
        int validRecords,

        @Schema(example = "3")
        int invalidRecords,

        @Schema(example = "2")
        int duplicateRecords,

        Instant startedAt,

        Instant finishedAt,

        Instant createdAt,

        @Schema(example = "admin")
        String createdBy,

        List<DataQualityIssueDTO> issues
) {
}
