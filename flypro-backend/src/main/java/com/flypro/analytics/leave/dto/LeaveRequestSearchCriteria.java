package com.flypro.analytics.leave.dto;

import com.flypro.analytics.common.enums.LeaveStatus;
import com.flypro.analytics.common.enums.LeaveType;

import java.time.LocalDate;

/**
 * Search / filter criteria for leave requests (admin view).
 */
public record LeaveRequestSearchCriteria(
        Long employeeId,
        Long departmentId,
        LeaveStatus status,
        LeaveType type,
        LocalDate from,
        LocalDate to,
        String keyword
) {
}
