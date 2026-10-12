package com.flypro.analytics.badge.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class BadgeSuspendedException extends ApiException {

    public BadgeSuspendedException(Long badgeId) {
        super("Badge is suspended: " + badgeId,
                HttpStatus.UNPROCESSABLE_ENTITY, "BADGE_SUSPENDED");
    }
}
