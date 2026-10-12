package com.flypro.analytics.importbatch.dto;

import com.flypro.analytics.common.enums.ImportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request to execute a previously uploaded import batch.
 */
@Schema(description = "Execute import request")
public record ImportRequestDTO(

        @NotNull(message = "Batch id is required")
        @Schema(example = "1", description = "Import batch identifier returned by upload")
        Long batchId
) {
}
