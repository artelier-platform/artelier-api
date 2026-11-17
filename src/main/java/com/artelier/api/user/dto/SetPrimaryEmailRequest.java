package com.artelier.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request used to change the primary email address of the authenticated user.
 *
 * <p>The selected email address must belong to the authenticated user
 * and must already be verified.</p>
 *
 * <p>The server is responsible for ensuring that only one email
 * address is marked as primary for the account.</p>
 *
 * @param emailId identifier of the email address to set as primary
 */
@Schema(description = "Request used to change the primary email address")
public record SetPrimaryEmailRequest(

        @Schema(
                description = "Identifier of the verified email address to set as primary",
                example = "550e8400-e29b-41d4-a716-446655440000",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        UUID emailId

) {
}