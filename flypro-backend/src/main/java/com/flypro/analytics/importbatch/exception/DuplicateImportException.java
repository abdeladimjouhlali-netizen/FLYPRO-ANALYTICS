package com.flypro.analytics.importbatch.exception;

import org.springframework.http.HttpStatus;

public class DuplicateImportException extends ImportException {

    public DuplicateImportException(Long batchId) {
        super("Import batch " + batchId + " has already been executed and cannot be re-run",
                HttpStatus.CONFLICT, "DUPLICATE_IMPORT");
    }
}
