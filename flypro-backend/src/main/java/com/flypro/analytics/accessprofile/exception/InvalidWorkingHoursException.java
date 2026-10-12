package com.flypro.analytics.accessprofile.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidWorkingHoursException extends ApiException {

    public InvalidWorkingHoursException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_WORKING_HOURS");
    }
}
