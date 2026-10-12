package com.flypro.analytics.alert.exception;

import com.flypro.analytics.common.enums.AlertType;
import org.springframework.http.HttpStatus;

public class DuplicateAlertException extends AlertException {

    public DuplicateAlertException(AlertType type) {
        super("An open alert of type " + type + " already exists for this context",
                HttpStatus.CONFLICT, "DUPLICATE_ALERT");
    }
}
