package com.artelier.api.repository;

import com.artelier.api.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);
    int rotateRefreshTokenHash(String currentHash, String newHash, Instant now);
}
