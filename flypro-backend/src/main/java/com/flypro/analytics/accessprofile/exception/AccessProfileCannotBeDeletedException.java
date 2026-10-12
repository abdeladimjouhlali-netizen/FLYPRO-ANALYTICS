package com.flypro.analytics.accessprofile.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AccessProfileCannotBeDeletedException extends ApiException {

    public AccessProfileCannotBeDeletedException(String message) {
        super(message, HttpStatus.CONFLICT, "ACCESS_PROFILE_CANNOT_BE_DELETED");
    }
}
