package com.flypro.analytics.security.bruteforce;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AccountTemporarilyLockedException extends ApiException {

    public AccountTemporarilyLockedException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "ACCOUNT_TEMPORARILY_LOCKED");
    }
}
