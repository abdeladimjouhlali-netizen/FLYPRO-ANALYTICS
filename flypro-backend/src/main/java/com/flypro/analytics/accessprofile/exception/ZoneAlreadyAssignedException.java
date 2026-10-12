package com.flypro.analytics.accessprofile.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class ZoneAlreadyAssignedException extends ApiException {

    public ZoneAlreadyAssignedException(Long profileId, Long zoneId) {
        super("Zone " + zoneId + " is already assigned to access profile " + profileId,
                HttpStatus.CONFLICT, "ZONE_ALREADY_ASSIGNED");
    }
}
