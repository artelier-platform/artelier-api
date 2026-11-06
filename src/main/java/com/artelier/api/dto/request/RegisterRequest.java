package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;


/**
 * Request used to register a new user account.
 *
 * <p>The request contains the user's username, credentials, personal
 * information, primary email address, and phone number.</p>
 *
 * <p>The username must contain between 1 and 12 printable ASCII characters,
 * without spaces. The email address is normalized to lowercase.</p>
 *
 * <p>The account is created with a pending verification status.
 * Email and phone verification are handled through separate processes.</p>
 *
 * @param username        unique username used to identify the account
 * @param password        account password (between 8 and 72 characters)
 * @param firstName       user's first name (maximum 58 characters)
 * @param lastName        user's last name (maximum 34 characters)
 * @param email           primary email address (required, must be unique)
 * @param phoneCountryCode international dialing code (maximum 4 characters)
 * @param phoneNumber     phone number without country calling code
 *                        (maximum 15 characters)
 */
@Schema(description = "User registration request")
public record RegisterRequest(

        @Schema(
                description = "Unique username. ASCII characters only, without spaces.",
                example = "Geronimo01",
                minLength = 1,
                maxLength = 12,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(max = 12)
        @Pattern(regexp = "^[\\x21-\\x3F\\x41-\\x7E]{1,12}$")
        String username,

        @Schema(
                description = "Account password",
                example = "StrongPass123",
                format = "password",
                minLength = 8,
                maxLength = 72,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @Schema(
                description = "User first name",
                example = "Geronimo",
                maxLength = 58,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(max = 58)
        String firstName,

        @Schema(
                description = "User last name",
                example = "Rodriguez",
                maxLength = 34,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(max = 34)
        String lastName,

        @Schema(
                description = "Primary email address. Must be unique.",
                example = "user@example.com",
                maxLength = 254,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @Schema(
                description = "Phone country calling code",
                example = "+57",
                maxLength = 4,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(max = 4)
        String phoneCountryCode,

        @Schema(
                description = "Phone number without country calling code",
                example = "3001234567",
                maxLength = 15,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(max = 15)
        String phoneNumber

) {

    public RegisterRequest {
        email = email == null
                ? null
                : email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "RegisterRequest[username=" + username
                + ", password=***"
                + ", firstName=" + firstName
                + ", lastName=" + lastName
                + ", email=" + email
                + ", phoneCountryCode=" + phoneCountryCode
                + ", phoneNumber=" + phoneNumber + "]";
    }
}