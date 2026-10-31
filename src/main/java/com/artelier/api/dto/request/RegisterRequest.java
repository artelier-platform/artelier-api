package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * User registration request.
 *
 * @param email    email address of the new account (required, must be unique)
 * @param password account password (required, at least 8 characters)
 * @param fullName full name of the user (required)
 * @param phone    optional phone number
 */
@Schema(description = "User registration request")
public record RegisterRequest(

        @Schema(
                description = "Email address of the new account. Must be unique.",
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
                maxLength = 72,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @Schema(
                description = "Full name of the user",
                example = "John Doe",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        String fullName,

        @Schema(
                description = "Optional phone number, including country code",
                example = "+573001234567"
        )
        String phone

) {

    public RegisterRequest {
        email = email == null
                ? null
                : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Masks the password so the credentials never end up in logs.
     */
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", password=***, fullName=" + fullName
                + ", phone=" + phone + "]";
    }
}
