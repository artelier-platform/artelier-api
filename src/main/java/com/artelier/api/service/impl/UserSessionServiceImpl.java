package com.artelier.api.service.impl;

import com.artelier.api.dto.response.IssuedSession;
import com.artelier.api.entity.User;
import com.artelier.api.enums.SessionRevocationReason;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.repository.UserSessionRepository;
import com.artelier.api.security.RefreshTokenHasher;
import com.artelier.api.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
@Transactional
public class UserSessionServiceImpl implements UserSessionService {
    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final RefreshTokenHasher refreshTokenHasher;

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Override
    public IssuedSession create(User user) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public IssuedSession rotate(String refreshToken) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void revoke(String refreshToken, SessionRevocationReason reason) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean isActive(UUID sessionId, UUID userId) {
        if (sessionId == null || userId == null) {
            throw ArtelierException.badRequest("Invalid session or user ID");
        }

        if (userRepository.findById(userId).isEmpty()) {
            throw ArtelierException.notFound("User not found for userId: " + userId);
        }

        return userSessionRepository.findBySessionUUIDAndUserUUID(sessionId, userId).isPresent();
    }
}
