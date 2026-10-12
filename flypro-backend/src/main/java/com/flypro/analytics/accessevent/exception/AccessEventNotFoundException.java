package com.flypro.analytics.accessevent.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AccessEventNotFoundException extends ApiException {

    public AccessEventNotFoundException(Long id) {
        super("Access event not found with id: " + id,
                HttpStatus.NOT_FOUND, "ACCESS_EVENT_NOT_FOUND");
    }
}
