package com.flypro.analytics.common.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception métier générique avec code HTTP associé.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
