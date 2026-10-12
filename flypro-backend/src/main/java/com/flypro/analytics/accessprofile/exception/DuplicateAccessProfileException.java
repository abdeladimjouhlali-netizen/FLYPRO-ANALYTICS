package com.flypro.analytics.accessprofile.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class DuplicateAccessProfileException extends ApiException {

    public DuplicateAccessProfileException(String name) {
        super("Access profile name already exists: " + name,
                HttpStatus.CONFLICT, "DUPLICATE_ACCESS_PROFILE");
    }
}
