
package com.artelier.api.service;

import com.artelier.api.entity.RefreshToken;
import com.artelier.api.entity.User;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.RefreshTokenRepository;
import com.artelier.api.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private RefreshTokenRepository repository;
    private RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenRepository.class);
        service = new RefreshTokenServiceImpl(repository);

        ReflectionTestUtils.setField(service, "refreshExpirationDays", 7L);
    }

    @Test
    void shouldCreateRefreshToken() {
        User user = new User();

        when(repository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken token = service.create(user);

        assertNotNull(token);
        assertNotNull(token.getToken());
        assertNotNull(token.getExpiresAt());
        assertEquals(user, token.getUser());
        assertTrue(token.getExpiresAt().isAfter(Instant.now()));

        verify(repository).save(any(RefreshToken.class));

        // Creating a token must not revoke other user tokens.
        verify(repository, never()).delete(any(RefreshToken.class));
    }

    @Test
    void shouldValidateToken() {
        RefreshToken token = new RefreshToken();
        token.setToken("valid");
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        when(repository.findByToken("valid"))
                .thenReturn(Optional.of(token));

        RefreshToken result = service.validate("valid");

        assertNotNull(result);
        assertEquals(token, result);

        verify(repository).findByToken("valid");
        verify(repository, never()).delete(any(RefreshToken.class));
    }

    @Test
    void shouldThrowIfTokenNotFound() {
        when(repository.findByToken("invalid"))
                .thenReturn(Optional.empty());

        assertThrows(
                ArtelierException.class,
                () -> service.validate("invalid")
        );
    }

    @Test
    void shouldThrowIfTokenExpired() {
        RefreshToken token = new RefreshToken();
        token.setToken("expired");
        token.setExpiresAt(Instant.now().minusSeconds(10));

        when(repository.findByToken("expired"))
                .thenReturn(Optional.of(token));

        assertThrows(
                ArtelierException.class,
                () -> service.validate("expired")
        );

        verify(repository).delete(token);
    }

    @Test
    void shouldConsumeValidToken() {
        RefreshToken token = new RefreshToken();
        token.setToken("valid");
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        when(repository.findByToken("valid"))
                .thenReturn(Optional.of(token));

        when(repository.consumeIfValid(eq("valid"), any(Instant.class)))
                .thenReturn(1);

        RefreshToken result = service.consume("valid");

        assertNotNull(result);
        assertEquals(token, result);

        verify(repository).consumeIfValid(eq("valid"), any(Instant.class));
    }

    @Test
    void shouldThrowIfConsumingUnknownToken() {
        when(repository.findByToken("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(
                ArtelierException.class,
                () -> service.consume("unknown")
        );

        verify(repository, never())
                .consumeIfValid(anyString(), any(Instant.class));
    }

    @Test
    void shouldThrowIfConsumingExpiredToken() {
        RefreshToken token = new RefreshToken();
        token.setToken("expired");
        token.setExpiresAt(Instant.now().minusSeconds(10));

        when(repository.findByToken("expired"))
                .thenReturn(Optional.of(token));

        assertThrows(
                ArtelierException.class,
                () -> service.consume("expired")
        );

        // Expired tokens must not reach the atomic consume operation.
        verify(repository, never())
                .consumeIfValid(anyString(), any(Instant.class));
    }

    @Test
    void shouldThrowIfTokenWasAlreadyConsumed() {
        RefreshToken token = new RefreshToken();
        token.setToken("already-used");
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        when(repository.findByToken("already-used"))
                .thenReturn(Optional.of(token));

        when(repository.consumeIfValid(
                eq("already-used"),
                any(Instant.class)
        )).thenReturn(0);

        assertThrows(
                ArtelierException.class,
                () -> service.consume("already-used")
        );
    }

    @Test
    void shouldRevokeTokenByToken() {
        service.revokeByToken("refresh-token");

        verify(repository).deleteByToken("refresh-token");
    }
}
