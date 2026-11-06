package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request containing the credentials required to authenticate a user.
 *
 * <p>Authentication can be performed using either the user's username
 * or a verified email address.</p>
 *
 * <p>The identifier is interpreted by the authentication service.
 * Username matching follows the configured username comparison policy,
 * while email matching uses the normalized email address.</p>
 *
 * @param identifier username or verified email address used for authentication
 * @param password   account password (between 8 and 72 characters)
 */
@Schema(description = "User login credentials")
public record LoginRequest(

        @Schema(
                description = "Username or verified email address",
                example = "Geronimo01",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(max = 254)
        String identifier,

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
        String password

) {

    @Override
    public String toString() {
        return "LoginRequest[identifier=" + identifier
                + ", password=***]";
    }
}