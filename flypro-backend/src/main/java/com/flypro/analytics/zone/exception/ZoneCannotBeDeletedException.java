package com.flypro.analytics.zone.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class ZoneCannotBeDeletedException extends ApiException {

    public ZoneCannotBeDeletedException(String message) {
        super(message, HttpStatus.CONFLICT, "ZONE_CANNOT_BE_DELETED");
    }
}
