package com.flypro.analytics.user.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends ApiException {

    public UserAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT, "USER_ALREADY_EXISTS");
    }
}
