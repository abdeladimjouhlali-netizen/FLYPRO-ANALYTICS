package com.flypro.analytics.leave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Payload used by an administrator to approve or reject a leave request.
 */
@Schema(description = "Leave request review payload")
public record LeaveRequestReviewDTO(

        @Size(max = 1000)
        @Schema(example = "Approuvé, bon congé")
        String comment
) {
}
