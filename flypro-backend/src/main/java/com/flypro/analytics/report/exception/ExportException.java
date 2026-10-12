package com.flypro.analytics.report.exception;

/**
 * Raised when a report cannot be rendered to the requested export format.
 */
public class ExportException extends RuntimeException {

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExportException(String message) {
        super(message);
    }
}
