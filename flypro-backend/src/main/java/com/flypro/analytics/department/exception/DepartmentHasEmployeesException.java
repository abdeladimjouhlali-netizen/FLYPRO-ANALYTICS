package com.flypro.analytics.department.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Raised when a hard delete is attempted while employees are still assigned.
 * Prefer soft-deactivation in that case.
 */
public class DepartmentHasEmployeesException extends ApiException {

    public DepartmentHasEmployeesException(Long id) {
        super(
                "Department " + id + " has assigned employees and cannot be permanently deleted",
                HttpStatus.CONFLICT,
                "DEPARTMENT_HAS_EMPLOYEES"
        );
    }
}
