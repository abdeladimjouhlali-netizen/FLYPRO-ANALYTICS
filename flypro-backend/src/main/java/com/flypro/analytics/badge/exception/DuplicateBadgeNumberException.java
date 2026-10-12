package com.flypro.analytics.badge.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class DuplicateBadgeNumberException extends ApiException {

    public DuplicateBadgeNumberException(String badgeNumber) {
        super("Badge number already exists: " + badgeNumber,
                HttpStatus.CONFLICT, "DUPLICATE_BADGE_NUMBER");
    }
}
