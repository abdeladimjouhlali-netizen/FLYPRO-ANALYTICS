package com.flypro.analytics.badge.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class BadgeExpiredException extends ApiException {

    public BadgeExpiredException(Long badgeId) {
        super("Badge is expired and cannot be used: " + badgeId,
                HttpStatus.UNPROCESSABLE_ENTITY, "BADGE_EXPIRED");
    }
}
