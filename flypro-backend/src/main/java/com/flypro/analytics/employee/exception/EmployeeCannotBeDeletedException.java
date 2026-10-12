package com.flypro.analytics.employee.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Raised when an employee cannot be permanently deleted (active badges / not archived).
 */
public class EmployeeCannotBeDeletedException extends ApiException {

    public EmployeeCannotBeDeletedException(String message) {
        super(message, HttpStatus.CONFLICT, "EMPLOYEE_CANNOT_BE_DELETED");
    }
}
