package com.artelier.api.auth.entity;

import com.artelier.api.auth.enums.SessionRevocationReason;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.UUID;
import com.artelier.api.user.entity.User;

/**
 * Persistent entity representing an active or historical user authentication session.
 *
 * <p>Tracks refresh token hashes, expiration times, usage timestamps,
 * and revocation states to support secure token rotation and threat detection mechanisms.</p>
 */
@Entity
@Table(
        name = "user_sessions",
        indexes = {
                @Index(name = "idx_user_sessions_user_id", columnList = "user_id"),
                @Index(name = "idx_user_sessions_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    /**
     * Unique identifier for the user session.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The user who owns and established this authentication session.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    /**
     * Cryptographic hash of the current active refresh token.
     */
    @NotBlank
    @Column(nullable = false, unique = true, length = 255)
    private String refreshTokenHash;

    /**
     * Cryptographic hash of the previously consumed refresh token,
     * retained temporarily for reuse detection and security auditing.
     */
    @Column(unique = true, length = 255)
    private String previousRefreshTokenHash;

    /**
     * Timestamp indicating when the session was created.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp indicating when the session was last utilized or rotated.
     */
    private Instant lastUsedAt;

    /**
     * Expiration timestamp of the session.
     */
    @Column(nullable = false)
    private Instant expiresAt;

    /**
     * Timestamp indicating when the session was explicitly or security-revoked, if applicable.
     */
    private Instant revokedAt;

    /**
     * The business or security reason why the session was revoked.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private SessionRevocationReason revocationReason;

    /**
     * Lifecycle callback executed before persisting a new entity,
     * automatically populating the creation timestamp.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Checks whether the session has passed its expiration time.
     *
     * @return {@code true} if the current time is at or after the expiration time; {@code false} otherwise
     */
    public boolean isExpired() {
        return !Instant.now().isBefore(expiresAt);
    }

    /**
     * Checks whether the session has been revoked.
     *
     * @return {@code true} if a revocation timestamp is present; {@code false} otherwise
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Checks whether the session is currently valid (neither expired nor revoked).
     *
     * @return {@code true} if the session is active and valid; {@code false} otherwise
     */
    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }

    /**
     * Revokes the session with the specified reason if it has not already been revoked.
     *
     * @param reason the {@link SessionRevocationReason} explaining the revocation
     */
    public void revoke(SessionRevocationReason reason) {
        if (revokedAt == null) {
            revokedAt = Instant.now();
            revocationReason = reason;
        }
    }
}