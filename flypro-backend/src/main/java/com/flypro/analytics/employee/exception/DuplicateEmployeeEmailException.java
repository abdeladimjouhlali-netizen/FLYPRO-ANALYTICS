package com.flypro.analytics.employee.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class DuplicateEmployeeEmailException extends ApiException {

    public DuplicateEmployeeEmailException(String email) {
        super("Employee email already exists: " + email,
                HttpStatus.CONFLICT, "DUPLICATE_EMPLOYEE_EMAIL");
    }
}
