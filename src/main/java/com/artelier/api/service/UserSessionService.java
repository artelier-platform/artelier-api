package com.artelier.api.service;

import com.artelier.api.dto.response.IssuedSession;
import com.artelier.api.entity.User;
import com.artelier.api.enums.SessionRevocationReason;

import java.util.UUID;

/**
 * Service interface for managing user sessions, token issuance, secure rotation,
 * lifecycle tracking, and session revocation workflows.
 */
public interface UserSessionService {

    /**
     * Creates and persists a new user session for the specified user.
     *
     * @param user the user for whom the session is being created
     * @return an {@link IssuedSession} containing the session details and the initial refresh token
     */
    IssuedSession create(User user);

    /**
     * Rotates an existing refresh token, invalidating the previous token
     * and issuing a new credential pair.
     *
     * @param refreshToken the current valid raw refresh token presented by the client
     * @return an {@link IssuedSession} containing the updated session details and the new refresh token
     */
    IssuedSession rotate(String refreshToken);

    /**
     * Revokes an active user session based on the provided refresh token and specified reason.
     *
     * @param refreshToken the raw refresh token associated with the session to be revoked
     * @param reason       the business or security {@link SessionRevocationReason} for the revocation
     */
    void revoke(String refreshToken, SessionRevocationReason reason);

    /**
     * Checks whether a specific session is currently active and valid for the given user.
     *
     * @param sessionId the unique identifier of the session
     * @param userId    the unique identifier of the user owning the session
     * @return {@code true} if the session exists, matches the user, and remains valid; {@code false} otherwise
     */
    boolean isActive(UUID sessionId, UUID userId);
}