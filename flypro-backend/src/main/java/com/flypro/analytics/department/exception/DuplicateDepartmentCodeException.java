package com.flypro.analytics.department.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Raised when a department code already exists.
 */
public class DuplicateDepartmentCodeException extends ApiException {

    public DuplicateDepartmentCodeException(String code) {
        super("Department code already exists: " + code, HttpStatus.CONFLICT, "DUPLICATE_DEPARTMENT_CODE");
    }
}
