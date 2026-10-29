
package com.artelier.api.dto.response;

import com.artelier.api.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
        name = "UserResponse",
        description = "A user account returned by the API"
)
public record UserResponse(

        @Schema(
                description = "Unique identifier of the user",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Email address of the account",
                example = "user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String email,

        @Schema(
                description = "Full name of the user",
                example = "John Doe",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String fullName,

        @Schema(
                description = "Phone number, including country code",
                example = "+573001234567",
                nullable = true
        )
        String phone,

        @Schema(
                description = "Whether the account is banned",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean banned,

        @Schema(
                description = "Role assigned to the user",
                example = "BUYER",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Role role

) {}
