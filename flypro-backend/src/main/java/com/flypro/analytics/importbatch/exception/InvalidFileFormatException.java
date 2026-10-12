package com.flypro.analytics.importbatch.exception;

import org.springframework.http.HttpStatus;

public class InvalidFileFormatException extends ImportException {

    public InvalidFileFormatException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_FILE_FORMAT");
    }
}
