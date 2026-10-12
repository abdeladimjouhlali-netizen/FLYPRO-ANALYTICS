package com.flypro.analytics.accessevent.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidAccessEventException extends ApiException {

    public InvalidAccessEventException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ACCESS_EVENT");
    }
}
