package com.flypro.analytics.accesspoint.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class DuplicateAccessPointNameException extends ApiException {

    public DuplicateAccessPointNameException(String name) {
        super("Access point name already exists: " + name,
                HttpStatus.CONFLICT, "DUPLICATE_ACCESS_POINT_NAME");
    }
}
