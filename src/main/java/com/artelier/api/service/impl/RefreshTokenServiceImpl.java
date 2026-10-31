package com.artelier.api.service.impl;

import com.artelier.api.entity.RefreshToken;
import com.artelier.api.entity.User;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.RefreshTokenRepository;
import com.artelier.api.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public RefreshToken create(User user) {

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(
                Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS)
        );

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public RefreshToken consume(String token) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() ->
                        ArtelierException.unauthorized("Invalid refresh token")
                );

        if (refreshToken.isExpired()) {
            throw ArtelierException.unauthorized(
                    "Refresh token expired, please login again"
            );
        }

        int deleted = refreshTokenRepository.consumeIfValid(
                token,
                Instant.now()
        );

        if (deleted != 1) {
            throw ArtelierException.unauthorized(
                    "Refresh token is invalid or has already been used"
            );
        }

        return refreshToken;
    }


    @Override
    public RefreshToken validate(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> ArtelierException.unauthorized("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw ArtelierException.unauthorized("Refresh token expired, please login again");
        }

        return refreshToken;
    }

    @Override
    public void revokeByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
}
