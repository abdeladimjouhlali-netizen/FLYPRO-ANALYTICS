package com.flypro.analytics.security.bruteforce;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService(3, 60);
    }

    @Test
    void locksAfterMaxFailures() {
        service.loginFailed("admin");
        service.loginFailed("admin");
        service.loginFailed("admin");

        assertThrows(AccountTemporarilyLockedException.class, () -> service.assertNotLocked("admin"));
    }

    @Test
    void successClearsFailures() {
        service.loginFailed("admin");
        service.loginFailed("admin");
        service.loginSucceeded("admin");

        assertDoesNotThrow(() -> service.assertNotLocked("admin"));
    }
}
