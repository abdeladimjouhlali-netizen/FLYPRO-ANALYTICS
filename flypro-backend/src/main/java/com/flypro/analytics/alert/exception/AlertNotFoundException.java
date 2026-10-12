package com.flypro.analytics.alert.exception;

import org.springframework.http.HttpStatus;

public class AlertNotFoundException extends AlertException {

    public AlertNotFoundException(Long id) {
        super("Alert not found with id: " + id, HttpStatus.NOT_FOUND, "ALERT_NOT_FOUND");
    }
}
