package com.flypro.analytics.dashboard.exception;

import org.springframework.http.HttpStatus;

public class InvalidDashboardFilterException extends DashboardException {

    public InvalidDashboardFilterException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_DASHBOARD_FILTER");
    }
}
