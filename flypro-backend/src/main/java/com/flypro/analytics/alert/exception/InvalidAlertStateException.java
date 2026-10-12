package com.flypro.analytics.alert.exception;

import org.springframework.http.HttpStatus;

public class InvalidAlertStateException extends AlertException {

    public InvalidAlertStateException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ALERT_STATE");
    }
}
