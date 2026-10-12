package com.flypro.analytics.employee.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class DuplicateEmployeeNumberException extends ApiException {

    public DuplicateEmployeeNumberException(String employeeNumber) {
        super("Employee number already exists: " + employeeNumber,
                HttpStatus.CONFLICT, "DUPLICATE_EMPLOYEE_NUMBER");
    }
}
