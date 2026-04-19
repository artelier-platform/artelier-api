package com.artelier.api.service;

import com.artelier.api.entity.RefreshToken;
import com.artelier.api.entity.User;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.RefreshTokenRepository;
import com.artelier.api.service.Impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

        when(repository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken token = service.create(user);

        assertNotNull(token.getToken());
        assertNotNull(token.getExpiresAt());

        verify(repository).deleteByUser(user);
        verify(repository).save(any());
    }

    @Test
    void shouldValidateToken() {
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        when(repository.findByToken("valid"))
                .thenReturn(Optional.of(token));

        RefreshToken result = service.validate("valid");

        assertNotNull(result);
    }

    @Test
    void shouldThrowIfTokenNotFound() {
        when(repository.findByToken("invalid"))
                .thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.validate("invalid"));
    }

    @Test
    void shouldThrowIfTokenExpired() {
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(Instant.now().minusSeconds(10)); // expirado

        when(repository.findByToken("expired"))
                .thenReturn(Optional.of(token));

        assertThrows(ArtelierException.class,
                () -> service.validate("expired"));

        verify(repository).delete(token);
    }

    @Test
    void shouldRevokeTokensByUser() {
        User user = new User();

        service.revokeByUser(user);

        verify(repository).deleteByUser(user);
    }
}