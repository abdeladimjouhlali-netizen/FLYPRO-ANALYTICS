package com.flypro.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Vérifie que le contexte Spring démarre avec le profil test.
 */
@SpringBootTest
@ActiveProfiles("test")
class FlyproBackendApplicationTest {

    @Test
    void contextLoads() {
        // Context load smoke test
    }
}
