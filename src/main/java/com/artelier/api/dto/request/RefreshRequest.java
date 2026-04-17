package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(
        name = "RefreshRequest",
        description = "Request used to obtain a new JWT access token using a valid refresh token"
)
public class RefreshRequest {

    @Schema(
            description = "Valid refresh token issued during login or previous refresh",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refreshTokenExample1234567890",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}