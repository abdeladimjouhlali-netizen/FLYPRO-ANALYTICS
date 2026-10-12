package com.flypro.analytics.leave.dto;

import com.flypro.analytics.common.enums.LeaveType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload submitted by an employee to request a leave period.
 */
@Schema(description = "Leave request creation payload")
public record LeaveRequestCreateDTO(

        @NotNull(message = "Start date is required")
        @Schema(example = "2026-09-15")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        @Schema(example = "2026-09-20")
        LocalDate endDate,

        @NotNull(message = "Leave type is required")
        @Schema(example = "ANNUAL")
        LeaveType type,

        @Size(max = 1000)
        @Schema(example = "Congé annuel")
        String reason
) {
}
