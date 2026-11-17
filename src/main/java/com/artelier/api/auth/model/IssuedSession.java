package com.artelier.api.auth.model;

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
                description = "Opaque refresh token used to obtain a new access token.",
                example = "dGhpcy1pcy1hLXNhbXBsZS1yZWZyZXNoLXRva2VuLWV4YW1wbGU=",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String refreshToken

) {}