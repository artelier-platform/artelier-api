package com.artelier.api.service;

import com.artelier.api.service.impl.LoginAttemptServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    @Test
    void shouldAllowFirstAttempts() {
        LoginAttemptServiceImpl service = new LoginAttemptServiceImpl();

        boolean result = service.tryConsume("test@example.com");

        assertTrue(result);
    }

    @Test
    void shouldBlockAfterLimit() {
        LoginAttemptServiceImpl service = new LoginAttemptServiceImpl();

        String key = "test@example.com";

        // consume 5 tokens
        for (int i = 0; i < 5; i++) {
            service.tryConsume(key);
        }

        boolean result = service.tryConsume(key);

        assertFalse(result);
    }

    @Test
    void shouldReturnRemainingTokens() {
        LoginAttemptServiceImpl service = new LoginAttemptServiceImpl();

        String key = "test@example.com";

        service.tryConsume(key);

        long tokens = service.getAvailableTokens(key);

        assertTrue(tokens < 5);
    }
}