package com.flypro.analytics.zone.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class DuplicateZoneException extends ApiException {

    public DuplicateZoneException(String name) {
        super("Zone name already exists: " + name, HttpStatus.CONFLICT, "DUPLICATE_ZONE");
    }
}
