package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login credentials.
 *
 * @param email    email address of the account
 * @param password account password (at least 8 characters)
 */
@Schema(description = "Login credentials")
public record LoginRequest(

        @Schema(
                description = "Email address of the account",
                example = "user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Email
        @NotBlank
        String email,

        @Schema(
                description = "Account password",
                example = "StrongPass123",
                format = "password",
                minLength = 8,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(min = 8)
        String password

) {

    /**
     * Masks the password so the credentials never end up in logs.
     */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
