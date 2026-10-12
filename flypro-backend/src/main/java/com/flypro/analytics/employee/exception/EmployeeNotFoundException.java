package com.flypro.analytics.employee.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class EmployeeNotFoundException extends ApiException {

    public EmployeeNotFoundException(Long id) {
        super("Employee not found with id: " + id, HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND");
    }
}
