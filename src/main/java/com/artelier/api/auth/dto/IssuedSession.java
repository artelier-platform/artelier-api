package com.artelier.api.auth.dto;

import com.artelier.api.auth.entity.UserSession;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "IssuedSession",
        description = "Response payload containing the newly issued user session details and the associated refresh token."
)
public record IssuedSession(

        @Schema(
                description = "The active user session entity containing metadata, user identifiers, and expiration details.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UserSession session,

        @Schema(
                description = "The cryptographic refresh token string used to obtain new access tokens once the current session expires.",
                example = "dGhpcy1pcy1hLXNhbXBsZS1yZWZyZXNoLXRva2VuLWV4YW1wbGU=",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String refreshToken

) {}