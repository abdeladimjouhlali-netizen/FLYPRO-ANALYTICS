package com.flypro.analytics.notification.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class TemplateException extends ApiException {
    public TemplateException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "TEMPLATE_ERROR");
    }

    public TemplateException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "TEMPLATE_ERROR");
        initCause(cause);
    }
}
