package com.flypro.analytics.accesspoint.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AccessPointCannotBeDeletedException extends ApiException {

    public AccessPointCannotBeDeletedException(String message) {
        super(message, HttpStatus.CONFLICT, "ACCESS_POINT_CANNOT_BE_DELETED");
    }
}
