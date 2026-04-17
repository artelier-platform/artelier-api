package com.artelier.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(
        name = "AuthResponse",
        description = "Response returned after successful authentication containing JWT tokens and session metadata"
)
public class AuthResponse {

    @Schema(
            description = "JWT access token used to authenticate API requests",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.accessTokenExample123456789"
    )
    private String token;

    @Schema(
            description = "User role assigned to the authenticated user",
            example = "BUYER",
            allowableValues = {
                    "ADMIN",
                    "BUYER"
            }
    )
    private String role;

    @Schema(
            description = "Refresh token used to obtain new access tokens without re-authentication",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refreshTokenExample987654321"
    )
    private String refreshToken;

    @Schema(
            description = "Access token expiration time in seconds",
            example = "3600"
    )
    private long expiresIn;
}