package com.flypro.analytics.badge.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class BadgeAlreadyRevokedException extends ApiException {

    public BadgeAlreadyRevokedException(Long badgeId) {
        super("Badge is already revoked: " + badgeId,
                HttpStatus.CONFLICT, "BADGE_ALREADY_REVOKED");
    }
}
