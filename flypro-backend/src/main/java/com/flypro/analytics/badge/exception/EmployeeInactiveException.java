package com.flypro.analytics.badge.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class EmployeeInactiveException extends ApiException {

    public EmployeeInactiveException(Long employeeId) {
        super("Employee is not active and cannot receive or keep an active badge: " + employeeId,
                HttpStatus.UNPROCESSABLE_ENTITY, "EMPLOYEE_INACTIVE");
    }
}
