package com.flypro.analytics.badge.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class BadgeNotFoundException extends ApiException {

    public BadgeNotFoundException(Long id) {
        super("Badge not found with id: " + id, HttpStatus.NOT_FOUND, "BADGE_NOT_FOUND");
    }
}
