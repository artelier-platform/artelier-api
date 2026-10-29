package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request used to obtain a new token pair from a valid refresh token, or to
 * terminate a session on logout.
 *
 * @param refreshToken valid refresh token issued at login, registration or the previous refresh
 */
@Schema(
        name = "RefreshRequest",
        description = "Request used to obtain a new JWT access token using a valid refresh token"
)
public record RefreshRequest(

        @Schema(
                description = "Valid refresh token issued during login or previous refresh",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refreshTokenExample1234567890",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Refresh token is required")
        String refreshToken

) {

    /**
     * Masks the token so the credential never ends up in logs.
     */
    @Override
    public String toString() {
        return "RefreshRequest[refreshToken=***]";
    }
}
