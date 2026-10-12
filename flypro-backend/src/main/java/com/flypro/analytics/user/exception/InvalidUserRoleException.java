package com.flypro.analytics.user.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidUserRoleException extends ApiException {

    public InvalidUserRoleException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_USER_ROLE");
    }
}
