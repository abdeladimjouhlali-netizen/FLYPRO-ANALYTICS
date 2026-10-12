package com.flypro.analytics.department.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Raised when a department name already exists.
 */
public class DuplicateDepartmentNameException extends ApiException {

    public DuplicateDepartmentNameException(String name) {
        super("Department name already exists: " + name, HttpStatus.CONFLICT, "DUPLICATE_DEPARTMENT_NAME");
    }
}
