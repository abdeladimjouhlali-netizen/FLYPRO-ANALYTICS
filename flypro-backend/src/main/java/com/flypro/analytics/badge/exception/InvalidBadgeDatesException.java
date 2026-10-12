package com.flypro.analytics.badge.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidBadgeDatesException extends ApiException {

    public InvalidBadgeDatesException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_BADGE_DATES");
    }
}
