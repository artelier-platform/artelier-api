package com.artelier.api.auth.dto;

import com.artelier.api.auth.enums.SessionRevocationReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response containing information about a user's authentication session.
 *
 * <p>Provides session lifecycle metadata, including creation,
 * last usage, expiration, and revocation information.</p>
 *
 * <p>The refresh token and its stored hash are never exposed.</p>
 *
 * @param id              unique session identifier
 * @param createdAt       timestamp when the session was created
 * @param lastUsedAt      timestamp when the session was last used
 * @param expiresAt       session expiration timestamp
 * @param revokedAt       timestamp when the session was revoked, if applicable
 * @param revocationReason reason why the session was revoked, if applicable
 */
@Schema(
        name = "UserSessionResponse",
        description = "Authentication session information"
)
public record UserSessionResponse(

        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(example = "2026-11-05T18:00:00Z")
        Instant createdAt,

        @Schema(example = "2026-11-05T19:00:00Z")
        Instant lastUsedAt,

        @Schema(example = "2026-11-12T18:00:00Z")
        Instant expiresAt,

        @Schema(example = "2026-11-05T19:30:00Z")
        Instant revokedAt,

        @Schema(
                description = "Reason why the session was revoked",
                example = "USER_LOGOUT",
                nullable = true
        )
        SessionRevocationReason revocationReason

) {
}