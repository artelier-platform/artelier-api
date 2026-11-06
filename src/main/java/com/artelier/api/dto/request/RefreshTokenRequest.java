package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;


/**
 * Request used to obtain a new authentication token pair from an existing
 * valid refresh token.
 *
 * <p>The refresh token identifies the user's authentication session.
 * The server validates the token, checks the associated session status,
 * and performs token rotation when the refresh operation succeeds.</p>
 *
 * <p>The refresh token must not be expired, revoked, or otherwise invalid.</p>
 *
 * @param refreshToken valid refresh token issued during registration,
 *                     login, or a previous token refresh
 */
@Schema(
        description = "Request used to refresh an authentication session"
)
public record RefreshTokenRequest(

        @Schema(
                description = "Valid refresh token issued during authentication",
                example = "eyJhbGciOiJIUzI1NiJ9.refreshTokenExample",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Refresh token is required")
        String refreshToken

) {

    @Override
    public String toString() {
        return "RefreshTokenRequest[refreshToken=***]";
    }
}