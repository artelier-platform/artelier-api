package com.artelier.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.artelier.api.user.dto.UserProfileResponse;

/**
 * Response returned after successful user authentication.
 *
 * <p>Contains the JWT access token, refresh token, token type,
 * access token lifetime, and authenticated user's profile.</p>
 *
 * <p>Both tokens are sensitive credentials and must never be logged
 * or exposed in application logs.</p>
 *
 * @param accessToken  JWT access token used to authenticate API requests
 * @param refreshToken token used to renew the authentication session
 * @param tokenType    authentication scheme used by the access token
 * @param expiresIn    access token lifetime in seconds
 * @param user         authenticated user's profile information
 */
@Schema(
        name = "AuthResponse",
        description = "Authentication response containing tokens and user information"
)
public record AuthResponse(

        @Schema(
                description = "JWT access token",
                example = "eyJhbGciOiJIUzI1NiJ9.example",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String accessToken,

        @Schema(
                description = "Refresh token used to renew the authentication session",
                example = "refreshTokenExample",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String refreshToken,

        @Schema(
                description = "Authentication scheme",
                example = "Bearer",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String tokenType,

        @Schema(
                description = "Access token expiration time in seconds",
                example = "3600",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long expiresIn,

        @Schema(
                description = "Authenticated user's profile",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UserProfileResponse user

) {

    @Override
    public String toString() {
        return "AuthResponse[accessToken=***, refreshToken=***"
                + ", tokenType=" + tokenType
                + ", expiresIn=" + expiresIn
                + ", user=" + user + "]";
    }
}