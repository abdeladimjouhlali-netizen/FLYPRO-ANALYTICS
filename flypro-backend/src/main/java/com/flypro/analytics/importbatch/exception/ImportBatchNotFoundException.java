package com.flypro.analytics.importbatch.exception;

import org.springframework.http.HttpStatus;

public class ImportBatchNotFoundException extends ImportException {

    public ImportBatchNotFoundException(Long id) {
        super("Import batch not found with id: " + id,
                HttpStatus.NOT_FOUND, "IMPORT_BATCH_NOT_FOUND");
    }
}
