package com.flypro.analytics.importbatch.exception;

import org.springframework.http.HttpStatus;

public class DataValidationException extends ImportException {

    public DataValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "DATA_VALIDATION_ERROR");
    }
}
