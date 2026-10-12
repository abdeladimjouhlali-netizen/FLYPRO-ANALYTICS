package com.flypro.analytics.importbatch.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Base exception for the Import Management module.
 */
public class ImportException extends ApiException {

    public ImportException(String message, HttpStatus status, String code) {
        super(message, status, code);
    }
}
