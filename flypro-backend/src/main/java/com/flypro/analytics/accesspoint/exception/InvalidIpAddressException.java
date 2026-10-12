package com.flypro.analytics.accesspoint.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidIpAddressException extends ApiException {

    public InvalidIpAddressException(String ipAddress) {
        super("Invalid IP address format: " + ipAddress,
                HttpStatus.BAD_REQUEST, "INVALID_IP_ADDRESS");
    }
}
