package com.flypro.analytics.notification.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class NotificationException extends ApiException {
    public NotificationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "NOTIFICATION_ERROR");
    }

    public NotificationException(String message, HttpStatus status) {
        super(message, status, "NOTIFICATION_ERROR");
    }
}
