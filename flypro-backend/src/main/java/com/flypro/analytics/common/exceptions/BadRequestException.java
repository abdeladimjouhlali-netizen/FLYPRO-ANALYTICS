package com.flypro.analytics.common.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Requête invalide (règles métier / état incohérent).
 */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }
}
