package com.flypro.analytics.badge.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class BadgeNotAssignedException extends ApiException {

    public BadgeNotAssignedException(Long badgeId) {
        super("Badge has no active assignment: " + badgeId,
                HttpStatus.CONFLICT, "BADGE_NOT_ASSIGNED");
    }
}
