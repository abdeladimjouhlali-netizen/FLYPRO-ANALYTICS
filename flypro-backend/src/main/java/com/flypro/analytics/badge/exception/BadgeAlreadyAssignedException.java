package com.flypro.analytics.badge.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class BadgeAlreadyAssignedException extends ApiException {

    public BadgeAlreadyAssignedException(Long badgeId) {
        super("Badge already has an active assignment: " + badgeId,
                HttpStatus.CONFLICT, "BADGE_ALREADY_ASSIGNED");
    }

    public BadgeAlreadyAssignedException(String message) {
        super(message, HttpStatus.CONFLICT, "BADGE_ALREADY_ASSIGNED");
    }
}
