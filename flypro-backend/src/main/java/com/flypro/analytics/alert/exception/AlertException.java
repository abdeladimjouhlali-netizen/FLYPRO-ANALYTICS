package com.flypro.analytics.alert.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Base exception for the Alert Management module.
 */
public class AlertException extends ApiException {

    public AlertException(String message, HttpStatus status, String code) {
        super(message, status, code);
    }
}
