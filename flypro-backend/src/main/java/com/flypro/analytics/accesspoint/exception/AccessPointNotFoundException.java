package com.flypro.analytics.accesspoint.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AccessPointNotFoundException extends ApiException {

    public AccessPointNotFoundException(Long id) {
        super("Access point not found with id: " + id,
                HttpStatus.NOT_FOUND, "ACCESS_POINT_NOT_FOUND");
    }
}
