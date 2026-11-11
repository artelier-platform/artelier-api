package com.artelier.api.repository;

import com.artelier.api.entity.UserSession;
import com.artelier.api.enums.SessionRevocationReason;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link UserSession} entities.
 * Handles persistence, token rotation, pessimistic locking, reuse detection, and batch revocation for authentication sessions.
 */
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    /**
     * Retrieves a user session by the cryptographic hash of its active refresh token.
     *
     * @param refreshTokenHash the secure hash of the refresh token
     * @return an {@link Optional} containing the user session if found
     */
    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    /**
     * Retrieves a user session by the cryptographic hash of its previously consumed refresh token.
     *
     * <p>Used for detecting refresh token reuse attacks or potential credential compromise.</p>
     *
     * @param previousRefreshTokenHash the secure hash of the previous refresh token
     * @return an {@link Optional} containing the user session if found
     */
    Optional<UserSession> findByPreviousRefreshTokenHash(String previousRefreshTokenHash);

    /**
     * Retrieves a specific session belonging to a specific user.
     *
     * @param sessionId the unique identifier of the session
     * @param userId    the unique identifier of the user owning the session
     * @return an {@link Optional} containing the matching user session
     */
    Optional<UserSession> findBySessionUUIDAndUserUUID(UUID sessionId, UUID userId);

    /**
     * Rotates an existing refresh token hash by updating it with a new hash value,
     * recording the usage timestamp, and shifting the current hash into the previous hash field.
     *
     * <p>This is a modifying query executed directly at the database level.</p>
     *
     * @param currentHash the current active refresh token hash
     * @param newHash     the new replacement refresh token hash
     * @param now         the timestamp when the rotation occurred
     * @return the number of updated database rows (1 if successful, 0 otherwise)
     */
    @Modifying
    @Query("""
    UPDATE UserSession s
    SET s.refreshTokenHash = :newHash,
        s.lastUsedAt = :now,
        s.previousRefreshTokenHash = :currentHash
    WHERE s.refreshTokenHash = :currentHash
""")
    int rotateRefreshTokenHash(
            @Param("currentHash") String currentHash,
            @Param("newHash") String newHash,
            @Param("now") Instant now
    );

    /**
     * Retrieves a user session by its unique identifier while applying a pessimistic write lock.
     *
     * @param sessionId the unique identifier of the session
     * @return an {@link Optional} containing the locked user session
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT s
    FROM UserSession s
    WHERE s.id = :sessionId
""")
    Optional<UserSession> findByIdForUpdate(
            @Param("sessionId") UUID sessionId
    );

    /**
     * Revokes all active sessions for a specific user.
     *
     * <p>Typically invoked during security events such as password updates, explicit global logouts,
     * or suspected credential compromise.</p>
     *
     * @param userId the unique identifier of the user whose sessions will be revoked
     * @param now    the timestamp of the revocation
     * @param reason the business or security reason for the revocation
     * @return the number of sessions that were revoked
     */
    @Modifying
    @Query("""
    UPDATE UserSession s
    SET s.revokedAt = :now,
        s.revocationReason = :reason
    WHERE s.user.id = :userId
      AND s.revokedAt IS NULL
""")
    int revokeAllByUserId(
            @Param("userId") UUID userId,
            @Param("now") Instant now,
            @Param("reason") SessionRevocationReason reason
    );
}