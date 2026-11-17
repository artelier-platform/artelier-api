package com.artelier.api.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request used to verify ownership of an email address.
 *
 * <p>The verification token is issued by the server and delivered
 * to the email address being verified.</p>
 *
 * <p>The server validates the token, checks its expiration and
 * associates the verification with the corresponding email record.</p>
 *
 * @param token valid email verification token
 */

@Schema(description = "Request used to verify an email address")
public record VerifyEmailRequest(

        @Schema(
                description = "Email verification token received by email",
                example = "eyJhbGciOiJIUzI1NiJ9.emailVerificationToken",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(max = 2048)
        String token

) {

    @Override
    public String toString() {
        return "VerifyEmailRequest[token=***]";
    }
}