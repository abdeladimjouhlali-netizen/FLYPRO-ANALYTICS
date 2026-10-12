package com.flypro.analytics.employee.dto;

import com.flypro.analytics.common.enums.EmployeeStatus;

/**
 * Advanced search criteria for employees.
 */
public record EmployeeSearchCriteria(
        String employeeNumber,
        String firstName,
        String lastName,
        Long departmentId,
        EmployeeStatus status,
        String position,
        String email,
        String keyword
) {
}
