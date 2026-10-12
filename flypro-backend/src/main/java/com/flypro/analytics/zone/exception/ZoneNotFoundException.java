package com.flypro.analytics.zone.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class ZoneNotFoundException extends ApiException {

    public ZoneNotFoundException(Long id) {
        super("Zone not found with id: " + id, HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND");
    }
}
