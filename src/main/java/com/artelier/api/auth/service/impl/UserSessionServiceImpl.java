package com.artelier.api.auth.service.impl;

import com.artelier.api.auth.dto.IssuedSession;
import com.artelier.api.user.entity.User;
import com.artelier.api.auth.entity.UserSession;
import com.artelier.api.auth.enums.SessionRevocationReason;
import com.artelier.api.shared.exception.ArtelierException;
import com.artelier.api.user.repository.UserRepository;
import com.artelier.api.auth.repository.UserSessionRepository;
import com.artelier.api.auth.security.UserJwtService;
import com.artelier.api.auth.security.RefreshTokenHasher;
import com.artelier.api.auth.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Implementation of {@link UserSessionService} responsible for managing user sessions,
 * issuing credentials, secure token rotation, and threat detection mechanisms such as 
 * token reuse and compromise mitigation.
 */
@RequiredArgsConstructor
@Service
@Transactional
public class UserSessionServiceImpl implements UserSessionService {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final UserJwtService UserJwtService;

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    /**
     * {@inheritDoc}
     *
     * @throws ArtelierException if the provided user is null
     */
    @Override
    public IssuedSession create(User user) {
        if (user == null) {
            throw ArtelierException.badRequest("User cannot be null");
        }

        return createUserSession(user);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Configured with {@code noRollbackFor = ArtelierException.class} to ensure audit records 
     * or security flags can be preserved even when unauthorized exceptions occur during rotation.</p>
     */
    @Override
    @Transactional(noRollbackFor = ArtelierException.class)
    public IssuedSession rotate(String refreshToken) {
        return rotateUserSession(refreshToken);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Revokes the target session and triggers a cascade global revocation across all active user sessions 
     * if the revocation reason represents a security threat.</p>
     */
    @Override
    public void revoke(String refreshToken, SessionRevocationReason reason) {
        UserSession userSession = getUserSession(refreshToken);

        userSession.revoke(reason);
        userSessionRepository.save(userSession);

        if (reason != SessionRevocationReason.USER_LOGOUT
                && reason != SessionRevocationReason.SESSION_EXPIRED) {
            userSessionRepository.revokeAllByUserId(
                    userSession.getUser().getId(), Instant.now(), reason);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive(UUID sessionId, UUID userId) {
        if (sessionId == null || userId == null) {
            throw ArtelierException.badRequest("Invalid session or user ID");
        }

        return userSessionRepository.findById(sessionId)
                .filter(session -> session.getUser().getId().equals(userId))
                .map(UserSession::isValid)
                .orElse(false);
    }

    // --------------------------------------------------
    // Private methods
    // --------------------------------------------------

    /**
     * Retrieves a user session using the secure hash of its refresh token.
     *
     * @param refreshToken raw refresh token presented by the client
     * @return the persistent session associated with the provided refresh token
     * @throws ArtelierException if the token is null or no matching session exists
     */
    private UserSession getUserSession(String refreshToken) {
        if (refreshToken == null) {
            throw ArtelierException.badRequest("Refresh token cannot be null");
        }

        String refreshTokenHash = refreshTokenHasher.hash(refreshToken);

        return userSessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> ArtelierException.unauthorized(
                        "No session found for the provided refresh token"
                ));
    }

    /**
     * Creates a new user session with a generated UUID and an associated refresh token.
     *
     * <p>Only the cryptographic hash of the refresh token is persisted in the database.</p>
     *
     * @param user the user who owns the new session
     * @return an {@link IssuedSession} containing the persisted entity and the raw token
     */
    private IssuedSession createUserSession(User user) {
        UUID sessionId = UUID.randomUUID();

        String refreshToken = UserJwtService.generateAccessToken(
                user.getId(),
                sessionId,
                user.getRole()
        );

        String refreshTokenHash = refreshTokenHasher.hash(refreshToken);
        Instant now = Instant.now();

        UserSession userSession = UserSession.builder()
                .id(sessionId)
                .user(user)
                .refreshTokenHash(refreshTokenHash)
                .createdAt(now)
                .lastUsedAt(now)
                .expiresAt(now.plus(refreshExpirationDays, ChronoUnit.DAYS))
                .build();

        userSessionRepository.save(userSession);

        return new IssuedSession(userSession, refreshToken);
    }

    /**
     * Rotates the refresh token associated with an existing user session.
     *
     * <p>Validates expiration, checks revocation status, detects potential token reuse attacks, 
     * and atomically rotates the stored hash while preserving the original session identifier.</p>
     *
     * @param currentRefreshToken raw refresh token to be rotated
     * @return an {@link IssuedSession} containing the updated session and the new raw token
     * @throws ArtelierException if the token is invalid, expired, revoked, or previously consumed
     */
    private IssuedSession rotateUserSession(String currentRefreshToken) {
        if (currentRefreshToken == null) {
            throw ArtelierException.badRequest("Refresh token cannot be null");
        }

        String currentRefreshTokenHash = refreshTokenHasher.hash(currentRefreshToken);

        UserSession userSession = userSessionRepository
                .findByRefreshTokenHash(currentRefreshTokenHash)
                .orElseThrow(() -> {
                    revokeAllSessionsIfTokenReused(currentRefreshTokenHash);
                    return ArtelierException.unauthorized(
                            "No session found for the provided refresh token");
                });

        if (userSession.isRevoked()) {
            userSessionRepository.revokeAllByUserId(
                    userSession.getUser().getId(), Instant.now(),
                    SessionRevocationReason.SECURITY_ACTION);
            throw ArtelierException.unauthorized("Session revoked");
        }

        if (userSession.getExpiresAt().isBefore(Instant.now())) {
            revoke(currentRefreshToken, SessionRevocationReason.SESSION_EXPIRED);
            throw ArtelierException.unauthorized("Session expired");
        }

        User user = userSession.getUser();
        UUID sessionId = userSession.getId();

        String refreshToken = UserJwtService.generateAccessToken(
                user.getId(),
                sessionId,
                user.getRole()
        );

        String refreshTokenHash = refreshTokenHasher.hash(refreshToken);

        int updatedRows = userSessionRepository.rotateRefreshTokenHash(
                currentRefreshTokenHash,
                refreshTokenHash,
                Instant.now()
        );

        if (updatedRows == 0) {
            throw ArtelierException.unauthorized(
                    "Refresh token already consumed"
            );
        }

        return new IssuedSession(userSession, refreshToken);
    }

    /**
     * Detects token reuse attempts by checking if a presented token matches a previously 
     * rotated hash. If matched, it triggers a global revocation for the user as a security safeguard.
     *
     * @param refreshTokenHash the hash of the suspicious refresh token
     * @throws ArtelierException with an unauthorized status if reuse is detected
     */
    private void revokeAllSessionsIfTokenReused(String refreshTokenHash) {
        userSessionRepository.findByPreviousRefreshTokenHash(refreshTokenHash)
                .ifPresent(session -> {
                    userSessionRepository.revokeAllByUserId(
                            session.getUser().getId(),
                            Instant.now(),
                            SessionRevocationReason.SECURITY_ACTION);
                    throw ArtelierException.unauthorized("Refresh token compromised");
                });
    }
}