
package com.artelier.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned after a successful authentication.
 *
 * @param token JWT access token
 * @param role user role
 * @param refreshToken token used to refresh authentication
 * @param expiresIn access token lifetime in seconds
 */
@Schema(
        name = "AuthResponse",
        description = "Authentication response containing JWT tokens and session metadata"
)
public record AuthResponse(

        @Schema(
                description = "JWT access token",
                example = "eyJhbGciOiJIUzI1NiJ9.example",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String token,

        @Schema(
                description = "User role",
                example = "BUYER",
                allowableValues = {"ADMIN", "BUYER"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String role,

        @Schema(
                description = "Refresh token",
                example = "refreshTokenExample",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String refreshToken,

        @Schema(
                description = "Access token expiration in seconds",
                example = "3600",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long expiresIn

) {
    @Override
    public String toString() {
        return "AuthResponse[token=***, role=" + role
                + ", refreshToken=***, expiresIn=" + expiresIn + "]";
    }
}
