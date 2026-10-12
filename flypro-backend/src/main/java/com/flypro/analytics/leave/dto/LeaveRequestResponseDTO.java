package com.flypro.analytics.leave.dto;

import com.flypro.analytics.common.enums.LeaveStatus;
import com.flypro.analytics.common.enums.LeaveType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Leave request representation returned by the API.
 */
@Schema(description = "Leave request")
public record LeaveRequestResponseDTO(
        Long id,
        Long employeeId,
        String employeeNumber,
        String employeeFirstName,
        String employeeLastName,
        Long departmentId,
        String departmentName,
        LocalDate startDate,
        LocalDate endDate,
        LeaveType type,
        String reason,
        LeaveStatus status,
        String reviewComment,
        String reviewedBy,
        Instant reviewedAt,
        Instant createdAt
) {
}
