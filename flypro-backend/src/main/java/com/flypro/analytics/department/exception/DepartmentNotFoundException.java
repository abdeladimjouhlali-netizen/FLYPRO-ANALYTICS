package com.flypro.analytics.department.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Raised when a department cannot be found.
 */
public class DepartmentNotFoundException extends ApiException {

    public DepartmentNotFoundException(Long id) {
        super("Department not found with id: " + id, HttpStatus.NOT_FOUND, "DEPARTMENT_NOT_FOUND");
    }
}
