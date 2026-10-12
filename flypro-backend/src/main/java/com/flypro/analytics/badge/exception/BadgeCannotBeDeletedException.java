package com.flypro.analytics.badge.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class BadgeCannotBeDeletedException extends ApiException {

    public BadgeCannotBeDeletedException(String message) {
        super(message, HttpStatus.CONFLICT, "BADGE_CANNOT_BE_DELETED");
    }
}
