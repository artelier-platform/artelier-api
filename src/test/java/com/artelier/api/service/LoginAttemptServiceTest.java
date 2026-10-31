
package com.artelier.api.service;

import com.artelier.api.service.impl.LoginAttemptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private LoginAttemptServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptServiceImpl();
    }

    @Test
    void shouldAllowFirstAttempt() {
        assertTrue(service.tryConsume("test@example.com"));
    }

    @Test
    void shouldAllowFiveAttempts() {
        String key = "test@example.com";

        for (int i = 0; i < 5; i++) {
            assertTrue(service.tryConsume(key));
        }
    }

    @Test
    void shouldBlockAfterFiveAttempts() {
        String key = "test@example.com";

        for (int i = 0; i < 5; i++) {
            service.tryConsume(key);
        }

        assertFalse(service.tryConsume(key));
    }

    @Test
    void shouldReturnRemainingTokens() {
        String key = "test@example.com";

        service.tryConsume(key);

        long tokens = service.getAvailableTokens(key);

        assertEquals(4, tokens);
    }

    @Test
    void shouldReturnFiveTokensForNewKey() {
        assertEquals(5, service.getAvailableTokens("new@example.com"));
    }

    @Test
    void shouldResetAttempts() {
        String key = "test@example.com";

        for (int i = 0; i < 5; i++) {
            service.tryConsume(key);
        }

        assertFalse(service.tryConsume(key));

        service.reset(key);

        assertTrue(service.tryConsume(key));
        assertEquals(4, service.getAvailableTokens(key));
    }

    @Test
    void shouldKeepAttemptsIndependentBetweenUsers() {
        String firstUser = "first@example.com";
        String secondUser = "second@example.com";

        for (int i = 0; i < 5; i++) {
            service.tryConsume(firstUser);
        }

        assertFalse(service.tryConsume(firstUser));

        assertTrue(service.tryConsume(secondUser));
        assertEquals(4, service.getAvailableTokens(secondUser));
    }

    @Test
    void shouldNotFailWhenResettingUnknownKey() {
        assertDoesNotThrow(() -> service.reset("unknown@example.com"));
    }
}
