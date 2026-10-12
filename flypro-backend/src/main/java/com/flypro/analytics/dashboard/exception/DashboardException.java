package com.flypro.analytics.dashboard.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Base exception for the Dashboard & Analytics module.
 */
public class DashboardException extends ApiException {

    public DashboardException(String message, HttpStatus status, String code) {
        super(message, status, code);
    }
}
