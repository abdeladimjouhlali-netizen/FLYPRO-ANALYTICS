package com.flypro.analytics.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Generic named count used by rankings and distributions.
 */
@Schema(description = "Named count")
public record NamedCountDTO(

        @Schema(example = "1")
        Long id,

        @Schema(example = "Main Door")
        String name,

        @Schema(example = "42")
        long count
) {
}
