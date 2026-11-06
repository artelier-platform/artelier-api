package com.artelier.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response containing information about an email address
 * associated with a user account.
 *
 * <p>Indicates whether the email address is primary and whether
 * ownership has been verified.</p>
 *
 * @param id         unique email record identifier
 * @param email      registered email address
 * @param isPrimary  indicates whether this is the primary email
 * @param isVerified indicates whether the email has been verified
 * @param verifiedAt timestamp when the email was verified
 * @param createdAt  timestamp when the email was added
 */
@Schema(
        name = "UserEmailResponse",
        description = "Information about a user's registered email address"
)
public record UserEmailResponse(

        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(example = "user@example.com")
        String email,

        @Schema(example = "true")
        boolean isPrimary,

        @Schema(example = "true")
        boolean isVerified,

        @Schema(example = "2026-11-05T18:30:00Z")
        Instant verifiedAt,

        @Schema(example = "2026-11-05T18:00:00Z")
        Instant createdAt

) {
}