package com.flypro.analytics.accessprofile.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AccessProfileMustHaveZoneException extends ApiException {

    public AccessProfileMustHaveZoneException() {
        super("Access profile must have at least one zone assigned",
                HttpStatus.UNPROCESSABLE_ENTITY, "ACCESS_PROFILE_MUST_HAVE_ZONE");
    }
}
