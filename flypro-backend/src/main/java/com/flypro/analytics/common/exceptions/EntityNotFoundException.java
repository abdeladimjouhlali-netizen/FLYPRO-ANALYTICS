package com.flypro.analytics.common.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Ressource introuvable (équivalent métier de EntityNotFoundException).
 */
public class EntityNotFoundException extends ApiException {

    public EntityNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "ENTITY_NOT_FOUND");
    }
}
