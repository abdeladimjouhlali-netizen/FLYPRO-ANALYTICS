package com.flypro.analytics.importbatch.exception;

import org.springframework.http.HttpStatus;

public class UnsupportedFileTypeException extends ImportException {

    public UnsupportedFileTypeException(String fileName) {
        super("Unsupported import file type for: " + fileName
                        + ". Supported formats: CSV, XLSX, JSON",
                HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_TYPE");
    }
}
