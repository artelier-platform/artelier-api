package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * Request used to add an email address to an existing user account.
 *
 * <p>The email address must be valid and globally unique.
 * A newly added email address starts as unverified and cannot
 * be used for authentication until verification is completed.</p>
 *
 * <p>The maximum number of email addresses per account is three,
 * including the primary email address.</p>
 *
 * @param email email address to add (required, maximum 254 characters)
 */
@Schema(description = "Request used to add an email address to the authenticated user's account")
public record AddUserEmailRequest(

        @Schema(
                description = "Email address to add to the account. Must be globally unique.",
                example = "secondary@example.com",
                maxLength = 254,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Email
        @Size(max = 254)
        String email

) {

    public AddUserEmailRequest {
        email = email == null
                ? null
                : email.trim().toLowerCase(Locale.ROOT);
    }
}