package com.flypro.analytics.accesspoint.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class DuplicateAccessPointCodeException extends ApiException {

    public DuplicateAccessPointCodeException(String code) {
        super("Access point code already exists: " + code,
                HttpStatus.CONFLICT, "DUPLICATE_ACCESS_POINT_CODE");
    }
}
