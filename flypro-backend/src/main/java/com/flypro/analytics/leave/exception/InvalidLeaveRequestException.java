package com.flypro.analytics.leave.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidLeaveRequestException extends ApiException {

    public InvalidLeaveRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_LEAVE_REQUEST");
    }
}
