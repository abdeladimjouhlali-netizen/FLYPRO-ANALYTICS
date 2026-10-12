package com.flypro.analytics.accessprofile.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AccessProfileNotFoundException extends ApiException {

    public AccessProfileNotFoundException(Long id) {
        super("Access profile not found with id: " + id,
                HttpStatus.NOT_FOUND, "ACCESS_PROFILE_NOT_FOUND");
    }
}
