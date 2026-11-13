package com.artelier.api.user.dto;

import com.artelier.api.user.enums.Role;
import com.artelier.api.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.artelier.api.user.entity.User;

/**
 * Response containing the profile information of a user.
 *
 * <p>Includes personal information, phone verification status,
 * profile image, account status, assigned role, registered email
 * addresses, and account creation timestamp.</p>
 *
 * <p>Sensitive authentication information, such as password hashes
 * and refresh token hashes, is never included.</p>
 *
 * @param id               unique user identifier
 * @param username         unique username
 * @param firstName        user's first name
 * @param lastName         user's last name
 * @param phoneCountryCode international phone country calling code
 * @param phoneNumber      user's phone number
 * @param phoneVerifiedAt  timestamp when the phone number was verified
 * @param profileImageUrl  user's profile image URL
 * @param status           current account status
 * @param role             assigned user role
 * @param emails           email addresses associated with the account
 * @param createdAt        account creation timestamp
 */
@Schema(
        name = "UserProfileResponse",
        description = "User profile information"
)
public record UserProfileResponse(

        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(example = "Geronimo01")
        String username,

        @Schema(example = "Geronimo")
        String firstName,

        @Schema(example = "Rodriguez")
        String lastName,

        @Schema(example = "+57")
        String phoneCountryCode,

        @Schema(example = "3001234567")
        String phoneNumber,

        @Schema(
                description = "Timestamp when the phone number was verified",
                example = "2026-11-05T18:30:00Z"
        )
        Instant phoneVerifiedAt,

        @Schema(
                description = "Profile image URL",
                example = "https://res.cloudinary.com/example/image/upload/profile.jpg"
        )
        String profileImageUrl,

        @Schema(example = "ACTIVE")
        UserStatus status,

        @Schema(example = "BUYER")
        Role role,

        @Schema(description = "Email addresses associated with the user")
        List<UserEmailResponse> emails,

        @Schema(example = "2026-11-05T18:00:00Z")
        Instant createdAt

) {
}